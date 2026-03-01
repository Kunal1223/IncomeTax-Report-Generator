package pks.IncomeTax.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.springframework.stereotype.Service;
import pks.IncomeTax.model.Employee;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Service
public class IncomeTaxReportService {

    private static final float EXTRA_PAGE_HEIGHT = 180f;
    private static final float AFTER_HEADER_GAP = 10f;

    // Reduce whitespace ABOVE black headers a bit (requested), but keep the below-gap unchanged
    // except for the "CALCULATION OF INCOME TAX PAYABLE" header.
    private static final float BEFORE_BLACK_HEADER_GAP = 4f;
    private static final float BEFORE_TAX_HEADER_GAP = 8f;
    private static final float AFTER_TAX_HEADER_GAP = 6f;

    private static final float PAGE_WIDTH_REDUCTION = 25f;

    private static final float CONTENT_X = 40f;
    private static final float CONTENT_W = 510f;
    private static final float RIGHT_PADDING = 6f;
    private static final float AMOUNT_RIGHT_X = CONTENT_X + CONTENT_W - RIGHT_PADDING;

    // Currency layout: keep ₹ in a fixed vertical column, and the number right-aligned.
    private static final float AMOUNT_NUMBER_COL_W = 70f;
    private static final float RUPEE_GAP = 4f;
    private static final float AMOUNT_RUPEE_X = AMOUNT_RIGHT_X - AMOUNT_NUMBER_COL_W - RUPEE_GAP;

    // For Section B/C item rows: show amounts slightly left of the main amount column.
    private static final float DETAIL_AMOUNT_SHIFT = 88f;
    private static final float DETAIL_AMOUNT_RIGHT_X = AMOUNT_RIGHT_X - DETAIL_AMOUNT_SHIFT;
    private static final float DETAIL_AMOUNT_RUPEE_X = DETAIL_AMOUNT_RIGHT_X - AMOUNT_NUMBER_COL_W - RUPEE_GAP;

    private static final float TAX_TABLE_NUMBER_COL_W = 70f;
    private static final float TAX_TABLE_W = 430f;
    private static final float BLACK_HEADER_WIDTH_FACTOR = 0.60f;

    // Small global font bump (applies to both text and numbers).
    private static final int FONT_SIZE_BUMP = 1;

    private record Fonts(PDFont normal, PDFont bold, boolean supportsRupee) {
    }

    private record TaxSummary(double taxBeforeRebate, double rebate87A, double netTax, double cess, double totalPayable) {
    }

    private long safe(Long v) {
        return v == null ? 0L : v.longValue();
    }

    private long roundToNearest10HalfUp(long value) {
        // Nearest 10 with .5 rounding up: 34->30, 35->40, 36->40.
        if (value >= 0) {
            return ((value + 5) / 10) * 10;
        }
        long abs = Math.abs(value);
        long roundedAbs = ((abs + 5) / 10) * 10;
        return -roundedAbs;
    }

    private long standardDeduction(Employee e) {
        int[] years = parseFinancialYear(e != null ? e.getFinancialYear() : null);

        // If FY is missing/unparseable, the PDF header defaults to FY 2025-2026.
        // Keep deduction consistent with that default.
        if (years == null) {
            return 75_000L;
        }

        // Default (and for FY 2023-2024, 2024-2025): 50,000
        long deduction = 50_000L;

        // FY 2025-2026: 75,000
        if (years[0] == 2025 && years[1] == 2026) {
            deduction = 75_000L;
        }
        return deduction;
    }

    private String buildYearHeader(Employee e) {
        String fyRaw = e != null ? e.getFinancialYear() : null;
        int[] years = parseFinancialYear(fyRaw);
        if (years == null) {
            return "Financial Year 2025-2026 (Assessment Year 2026-2027)";
        }
        int fyStart = years[0];
        int fyEnd = years[1];
        int ayStart = fyStart + 1;
        int ayEnd = fyEnd + 1;
        return "Financial Year " + fyStart + "-" + fyEnd + " (Assessment Year " + ayStart + "-" + ayEnd + ")";
    }

    private int[] parseFinancialYear(String fy) {
        if (fy == null) return null;
        String s = fy.trim();
        if (s.isEmpty()) return null;

        // Accept formats like "2025-2026", "2025-26", and with en dash.
        s = s.replace('–', '-');
        String[] parts = s.split("-");
        if (parts.length != 2) return null;

        try {
            int start = Integer.parseInt(parts[0].trim());
            String endPart = parts[1].trim();
            int end;
            if (endPart.length() == 2) {
                // Expand 2-digit end year using the start century.
                int century = (start / 100) * 100;
                end = century + Integer.parseInt(endPart);
            } else {
                end = Integer.parseInt(endPart);
            }
            if (start < 1900 || end < 1900) return null;
            return new int[] { start, end };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String generate(Employee e) throws Exception {

        Path dir = Path.of("reports");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        File file = dir.resolve("Schedule_of_Income_Tax.pdf").toFile();

        try (PDDocument doc = new PDDocument()) {
            Fonts fonts = loadFonts(doc);
            PDRectangle pageSize = new PDRectangle(
                    PDRectangle.A4.getWidth() - PAGE_WIDTH_REDUCTION,
                    PDRectangle.A4.getHeight() + EXTRA_PAGE_HEIGHT
            );
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float pw = page.getMediaBox().getWidth();
                float ph = page.getMediaBox().getHeight();

                /* ---------- OUTER BORDER ---------- */
                cs.setStrokingColor(0f, 0f, 0f);
                cs.setLineWidth(1.8f);
                cs.setLineDashPattern(new float[]{6, 3}, 0);
                cs.addRect(20, 20, pw - 40, ph - 40);
                cs.stroke();
                cs.setLineDashPattern(new float[]{}, 0);
                cs.setLineWidth(1.0f);

                /* ---------- HEADER ---------- */
                center(cs, fonts.bold(), 12, pw, ph - 60, "Schedule of Income Tax");
                center(cs, fonts.normal(), 9, pw, ph - 70, "(Fill in four copies)");
                center(cs, fonts.normal(), 9, pw, ph - 86, buildYearHeader(e));

                float y = ph - 110;
                float lx = 40;

                /* ---------- BASIC DETAILS ---------- */
                y = labelValue(cs, fonts, y, lx, 260, "Name of the Employee:", e.getName() != null ? e.getName() : "", true);
                y = labelValue(cs, fonts, y, lx, 260, "Designation:", e.getPost() != null ? e.getPost() : "", false);
                y = labelValue(cs, fonts, y, lx, 260, "Permanent Account Number (PAN):", e.getPan() != null ? e.getPan() : "", true);
                // As per UI mapping: Office Name = department, Office TAN = employerTan, Treasury Name = treasuryName
                y = labelValue(cs, fonts, y, lx, 260, "Office Name:", e.getDepartment() != null ? e.getDepartment() : "", false);
                y = labelValue(cs, fonts, y, lx, 260, "Office TAN:", e.getEmployerTan() != null ? e.getEmployerTan() : "", false);
                y = labelValue(cs, fonts, y, lx, 260, "Treasury Name:", e.getTreasuryName() != null ? e.getTreasuryName() : "", false);

                /* ---------- SECTION A ---------- */
                y -= BEFORE_BLACK_HEADER_GAP;
                blackHeader(cs, fonts, lx, y, CONTENT_W, "(A) Income from Salary :");
                y -= (14 + AFTER_HEADER_GAP); // 14 = header height

                // Use employee-provided values (fallback to 0)
                long basic = safe(e.getBasicPay());
                long daVal = safe(e.getDa());
                long hraVal = safe(e.getHra());
                long med = safe(e.getMedicalAllowances());
                long taVal = safe(e.getTa());
                long daOnTransport = safe(e.getDaOnTransportAllowance());
                long specialPay = safe(e.getSpecialPay());
                long arrearDA = safe(e.getArrearDearnessAllowance());
                long arrearPay = safe(e.getArrearPayAndAllowances());
                
                String salaryLabel = "(1) Basic Salary " + buildSalaryPeriod(e) + ":";
                y = money(cs, fonts, y, salaryLabel, basic, false);
                y = money(cs, fonts, y, "(2) Dearness Allowance:", daVal, false);
                y = money(cs, fonts, y, "(3) House Rent Allowance:", hraVal, false);
                y = money(cs, fonts, y, "(4) Medical Allowance:", med, false);
                y = money(cs, fonts, y, "(5) Transport Allowance:", taVal, false);
                y = money(cs, fonts, y, "(6) DA on Transport Allowance:", daOnTransport, false);
                y = money(cs, fonts, y, "(7) Special Pay/Bonus/Honorarium/Nursing Allowance/Other Allowances:", specialPay, false);
                y = money(cs, fonts, y, "(8) Arrear of Dearness Allowance:", arrearDA, false);
                y = money(cs, fonts, y, "(9) Arrear of Pay and Allowances:", arrearPay, false);

                long totalIncomeFromSalary = basic + daVal + hraVal + med + taVal + daOnTransport + specialPay + arrearDA + arrearPay;
                y = moneyBoldLabel(cs, fonts, y, "(10) Total Income from Salary:", totalIncomeFromSalary, true);

                long standardDeduction = standardDeduction(e);
                y = money(cs, fonts, y, "(-) Less : Standard deduction u/s 16(1)", standardDeduction, false);
                long incomeFromSalaryAfterDeduction = Math.max(0L, totalIncomeFromSalary - standardDeduction);
                y = moneyBoldLabel(cs, fonts, y, "Total Income from Salary", incomeFromSalaryAfterDeduction, true);

                /* ---------- SECTION B ---------- */
                y -= BEFORE_BLACK_HEADER_GAP;
                blackHeader(cs, fonts, lx, y, CONTENT_W, "(B) Income from House Property :");
                y -= (14 + AFTER_HEADER_GAP);

                long houseRentIncome = safe(e.getIncomeFromHouseRent());
                long housingLoanInterest = safe(e.getInterestOnHousingLoan());
                long totalHouseProperty = houseRentIncome - housingLoanInterest;
                y = moneyAt(cs, fonts, y, "(i) Income from House Rent", houseRentIncome, false, DETAIL_AMOUNT_RUPEE_X, DETAIL_AMOUNT_RIGHT_X);
                y = moneyAt(cs, fonts, y, "(ii) Interest on Housing Loan (u/s 24b)", housingLoanInterest, false, DETAIL_AMOUNT_RUPEE_X, DETAIL_AMOUNT_RIGHT_X);
                y = moneyBoldLabel(cs, fonts, y, "Total Income from House Property", totalHouseProperty, true);

                /* ---------- SECTION C ---------- */
                y -= BEFORE_BLACK_HEADER_GAP;
                blackHeader(cs, fonts, lx, y, CONTENT_W, "(C) Income from Other Sources :");
                y -= (14 + AFTER_HEADER_GAP);

                long interestSaving = safe(e.getInterestOnSaving());
                long interestFD = safe(e.getInterestOnFixedDeposit());
                long otherIncome = safe(e.getAnyOtherIncome());
                long totalOtherSources = interestSaving + interestFD + otherIncome;
                y = moneyAt(cs, fonts, y, "(i) Interest on Saving A/c of Bank/Post Office", interestSaving, false, DETAIL_AMOUNT_RUPEE_X, DETAIL_AMOUNT_RIGHT_X);
                y = moneyAt(cs, fonts, y, "(ii) Interest on Fixed Deposit / Recurring Deposit / KVP etc.", interestFD, false, DETAIL_AMOUNT_RUPEE_X, DETAIL_AMOUNT_RIGHT_X);
                y = moneyAt(cs, fonts, y, "(iii) Any other Income / Commission / etc.", otherIncome, false, DETAIL_AMOUNT_RUPEE_X, DETAIL_AMOUNT_RIGHT_X);
                y = moneyBoldLabel(cs, fonts, y, "Total Income from Other Sources", totalOtherSources, true);

                /* ---------- GROSS TOTAL ---------- */
                y -= BEFORE_BLACK_HEADER_GAP;
                long grossTotalIncome = incomeFromSalaryAfterDeduction + totalHouseProperty + totalOtherSources; // salary(after std deduction) + house + other
                blackHeaderWithAmount(cs, fonts, lx, y, CONTENT_W, "GROSS TOTAL INCOME", grossTotalIncome);
                y -= (14 + AFTER_HEADER_GAP);
                long grossTotalIncomeRounded = roundToNearest10HalfUp(grossTotalIncome);
                y = moneyBoldLabel(cs, fonts, y, "GROSS TOTAL INCOME (ROUNDED OFF UPTO " + currencyMark(fonts) + " 10/-)", grossTotalIncomeRounded, true);

                /* ---------- TAX TABLE ---------- */
                y -= BEFORE_TAX_HEADER_GAP;
                blackHeaderExact(cs, fonts, lx, y, TAX_TABLE_W, "CALCULATION OF INCOME TAX PAYABLE");
                y -= (14 + AFTER_TAX_HEADER_GAP);
                y = drawTaxTable(cs, fonts, lx, y, grossTotalIncomeRounded);

                /* ---------- FOOT NOTES ---------- */
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                    "* Tax-free Income: Basic exemption limit is " + currencyMark(fonts) + " 4,00,000 for male/female taxpayers below 60 years of age.");
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                    "Less: Tax Relief under Section 87A under New Tax Regime (Rebate up to " + currencyMark(fonts) + " 60,000 for total income up to " + currencyMark(fonts) + " 12 lakhs).");
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                    "If total income exceeds " + currencyMark(fonts) + " 12 lakhs, Marginal Relief will be granted.");

                /* ---------- FINAL TOTALS ---------- */
                y -= 18;
                TaxSummary tax = computeTaxSummary(grossTotalIncomeRounded);
                y = money(cs, fonts, y, "Net Income Tax Payable", tax.netTax(), true);
                y = money(cs, fonts, y,
                    "Add : 4% Health and Education Cess on " + currencyMark(fonts) + " " + fmtNumber(tax.netTax()),
                    tax.cess(), false);
                y = moneyBoldLabel(cs, fonts, y, "Total Income Tax and Health & Education Cess Payable", tax.totalPayable(), true);
                y = money(cs, fonts, y, "Less: Income Tax paid / deducted monthly from salary (-)", 0L, false);
                y = money(cs, fonts, y,
                    "Balance: Income Tax deposited / deducted through Salary for the month of February",
                    0L, false);
                String fyShort = buildFinancialYearShort(e);
                y = moneyBoldLabel(cs, fonts, y,
                    "Payable Income Tax and Health & Education Cess for Financial Year " + fyShort,
                    tax.totalPayable(), true);

                /* ---------- FOOTER ---------- */
                // Place: use department if available
                String place = e.getDepartment() != null ? e.getDepartment() : "";
                text(cs, fonts.normal(), 9, lx, 70, "Place: " + place);
                text(cs, fonts.normal(), 9, lx, 55, "Date : " + LocalDate.now());

                text(cs, fonts.normal(), 9, pw - 240, 70, "Signature and Seal");
                text(cs, fonts.normal(), 9, pw - 260, 55, "Drawing & Disbursing Officer");
                text(cs, fonts.normal(), 9, lx, 40, "Taxpayer's Signature");
            }
            doc.save(file);
        }
        return file.getName();
    }

    private String buildSalaryPeriod(Employee e) {

        int[] years = parseFinancialYear(
                e != null ? e.getFinancialYear() : null
        );

        if (years == null) {
            return "(01-03-2025 to 28-02-2026)";
        }

        int startYear = years[0];
        int endYear = years[1];

        // February last day handling (leap year safe)
        int febLastDay = java.time.Year.isLeap(endYear) ? 29 : 28;

        return String.format(
                "(01-03-%d to %02d-02-%d)",
                startYear,
                febLastDay,
                endYear
        );
    }

    /* ===================== TAX TABLE ===================== */

    private float drawTaxTable(PDPageContentStream cs, Fonts fonts, float x, float y, long taxableIncome) throws Exception {

        float rowH = 16;
        // Keep table slightly narrower than the main content width for better structure.
        float tableW = TAX_TABLE_W;
        float[] cols = {x, x + 70, x + 260, x + 340, x + tableW};

        int rows = 8;
        float tableH = rows * rowH;

        cs.addRect(x, y - tableH, tableW, tableH);
        for (int i = 1; i < rows; i++) {
            cs.moveTo(x, y - i * rowH);
            cs.lineTo(x + tableW, y - i * rowH);
        }
        for (float cx : cols) {
            cs.moveTo(cx, y);
            cs.lineTo(cx, y - tableH);
        }
        cs.stroke();

        double[] slabTax = computeSlabTaxes(taxableIncome);
        double totalTax = 0.0;
        for (double v : slabTax) totalTax += v;

        String curr = currencyMark(fonts);

        String[][] data = {
            {"(i) First", curr + " 1 to " + curr + " 4,00,000", "NIL", "NIL"},
            {"(ii) Next", curr + " 4,00,001 to " + curr + " 8,00,000", "5.00%", slabTax[1] <= 0 ? "NIL" : fmtNumber(slabTax[1])},
            {"(iii) Next", curr + " 8,00,001 to " + curr + " 12,00,000", "10.00%", slabTax[2] <= 0 ? "NIL" : fmtNumber(slabTax[2])},
            {"(iv) Next", curr + " 12,00,001 to " + curr + " 16,00,000", "15.00%", slabTax[3] <= 0 ? "NIL" : fmtNumber(slabTax[3])},
            {"(v) Next", curr + " 16,00,001 to " + curr + " 20,00,000", "20.00%", slabTax[4] <= 0 ? "NIL" : fmtNumber(slabTax[4])},
            {"(vi) Next", curr + " 20,00,001 to " + curr + " 24,00,000", "25.00%", slabTax[5] <= 0 ? "NIL" : fmtNumber(slabTax[5])},
            {"(vii) Balance", curr + " 24,00,001 to above", "30.00%", slabTax[6] <= 0 ? "NIL" : fmtNumber(slabTax[6])},
            {"", "", "Total :", totalTax <= 0 ? "NIL" : fmtNumber(totalTax)}
        };

        float ty = y - 12;
        float cellRightX = cols[4] - RIGHT_PADDING;
        float cellRupeeX = cellRightX - TAX_TABLE_NUMBER_COL_W - RUPEE_GAP;
        for (String[] r : data) {
            text(cs, fonts.normal(), 9, cols[0] + 2, ty, r[0]);
            text(cs, fonts.normal(), 9, cols[1] + 2, ty, r[1]);
            text(cs, fonts.normal(), 9, cols[2] + 2, ty, r[2]);
            drawCurrencyAmountRight(cs, fonts, fonts.bold(), 9, cellRupeeX, cellRightX, ty, r[3]);
            ty -= rowH;
        }
        return y - tableH;
    }

    private double[] computeSlabTaxes(long taxableIncome) {
        long income = Math.max(0L, taxableIncome);

        // Slabs as per the table shown in the PDF.
        long[] lower = {0L, 400_000L, 800_000L, 1_200_000L, 1_600_000L, 2_000_000L, 2_400_000L};
        long[] upper = {400_000L, 800_000L, 1_200_000L, 1_600_000L, 2_000_000L, 2_400_000L, Long.MAX_VALUE};
        double[] rate = {0.00, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30};

        double[] slabTax = new double[rate.length];
        for (int i = 0; i < rate.length; i++) {
            long taxableInSlab;
            if (upper[i] == Long.MAX_VALUE) {
                taxableInSlab = Math.max(0L, income - lower[i]);
            } else {
                taxableInSlab = Math.max(0L, Math.min(income, upper[i]) - lower[i]);
            }
            slabTax[i] = taxableInSlab * rate[i];
        }
        return slabTax;
    }

    private TaxSummary computeTaxSummary(long taxableIncome) {
        double[] slabTax = computeSlabTaxes(taxableIncome);
        double taxBeforeRebate = 0.0;
        for (double v : slabTax) taxBeforeRebate += v;

        // As per requirement: if income is <= 12L, rebate u/s 87A up to Rs. 60,000.
        // With these slabs, tax up to 12L is <= 60,000, so net becomes 0.
        double rebate87A = (taxableIncome <= 1_200_000L)
                ? Math.min(60_000.0, taxBeforeRebate)
                : 0.0;

        double netTax = Math.max(0.0, taxBeforeRebate - rebate87A);
        double cess = (taxableIncome > 1_200_000L) ? (netTax * 0.04) : 0.0;
        double totalPayable = netTax + cess;

        return new TaxSummary(taxBeforeRebate, rebate87A, netTax, cess, totalPayable);
    }

    private String buildFinancialYearShort(Employee e) {
        String fyRaw = e != null ? e.getFinancialYear() : null;
        int[] years = parseFinancialYear(fyRaw);
        if (years == null) {
            if (fyRaw != null && !fyRaw.trim().isEmpty()) return fyRaw.trim();
            return "2025-26";
        }
        int start = years[0];
        int end = years[1];
        return String.format("%d-%02d", start, end % 100);
    }

    /* ===================== HELPERS ===================== */

    private int effectiveFontSize(int s) {
        // Only bump the smaller body fonts; keep larger header fonts stable.
        if (s >= 12) return s;
        return Math.max(1, s + FONT_SIZE_BUMP);
    }

    private void textRaw(PDPageContentStream cs, PDFont f, int s, float x, float y, String t) throws Exception {
        if (t == null) t = "";
        cs.beginText();
        cs.setFont(f, s);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private void blackHeader(PDPageContentStream cs, Fonts fonts,
                         float x, float y, float w, String text) throws Exception {

        blackHeaderExact(cs, fonts, x, y, w * BLACK_HEADER_WIDTH_FACTOR, text);
    }

    private void blackHeaderExact(PDPageContentStream cs, Fonts fonts,
                             float x, float y, float barWidth, String text) throws Exception {

        float h = 14f;
        float fontSize = effectiveFontSize(9);
        float bottomPadding = 0f;

        /* ---- Draw black background ---- */
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.addRect(x, y - h, barWidth, h);
        cs.fill();

        /* ---- Calculate vertical centering ---- */
        PDFont font = fonts.bold();
        float ascent = font.getFontDescriptor().getAscent() / 1000 * fontSize;
        float descent = font.getFontDescriptor().getDescent() / 1000 * fontSize;

        float textHeight = ascent - descent;
        float textY = y - h + (h - textHeight) / 2 - descent + bottomPadding;

        /* ---- Draw text ---- */
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x + 6, textY);
        cs.showText(text);
        cs.endText();

        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    private void blackHeaderWithAmount(
            PDPageContentStream cs,
            Fonts fonts,
            float x,
            float y,
            float w,
            String headerText,
            double amount
    ) throws Exception {

        float h = 14f;

        // Draw the black bar + header text.
        blackHeader(cs, fonts, x, y, w, headerText);

        // Vertically center the amount on the same header row.
        float fontSize = effectiveFontSize(9);
        PDFont font = fonts.bold();
        float ascent = font.getFontDescriptor().getAscent() / 1000 * fontSize;
        float descent = font.getFontDescriptor().getDescent() / 1000 * fontSize;
        float textHeight = ascent - descent;
        float textY = y - h + (h - textHeight) / 2 - descent;

        drawCurrencyAmountRight(cs, fonts, fonts.bold(), 9, AMOUNT_RUPEE_X, AMOUNT_RIGHT_X, textY, fmtNumber(amount));
    }

    private float labelValue(PDPageContentStream cs, Fonts fonts, float y, float lx, float vx, String l, String v, boolean valueBold) throws Exception {
        PDFont labelFont = fonts.normal();
        int labelSize = 9;
        text(cs, labelFont, labelSize, lx, y, l);
        drawLeaderLine(cs, labelFont, labelSize, lx, y, l, vx - 4);
        text(cs, valueBold ? fonts.bold() : fonts.normal(), 9, vx, y, v == null ? "" : v);
        return y - 14;
    }

    private float money(PDPageContentStream cs, Fonts fonts, float y, String l, double v, boolean bold) throws Exception {
        PDFont labelFont = fonts.normal();
        int labelSize = 9;
        text(cs, labelFont, labelSize, CONTENT_X, y, l);
        drawLeaderLine(cs, labelFont, labelSize, CONTENT_X, y, l, AMOUNT_RUPEE_X - 6);
        PDFont f = bold ? fonts.bold() : fonts.normal();
        drawCurrencyAmountRight(cs, fonts, f, 9, AMOUNT_RUPEE_X, AMOUNT_RIGHT_X, y, fmtNumber(v));
        return y - 14;
    }

    private float moneyAt(
            PDPageContentStream cs,
            Fonts fonts,
            float y,
            String l,
            double v,
            boolean bold,
            float rupeeX,
            float rightX
    ) throws Exception {
        PDFont labelFont = fonts.normal();
        int labelSize = 9;
        text(cs, labelFont, labelSize, CONTENT_X, y, l);
        drawLeaderLine(cs, labelFont, labelSize, CONTENT_X, y, l, rupeeX - 6);
        PDFont f = bold ? fonts.bold() : fonts.normal();
        drawCurrencyAmountRight(cs, fonts, f, 9, rupeeX, rightX, y, fmtNumber(v));
        return y - 14;
    }

    private float moneyBoldLabel(PDPageContentStream cs, Fonts fonts, float y, String l, double v, boolean boldAmount) throws Exception {
        PDFont labelFont = fonts.bold();
        int labelSize = 9;
        text(cs, labelFont, labelSize, CONTENT_X, y, l);
        drawLeaderLine(cs, labelFont, labelSize, CONTENT_X, y, l, AMOUNT_RUPEE_X - 6);
        PDFont f = boldAmount ? fonts.bold() : fonts.normal();
        drawCurrencyAmountRight(cs, fonts, f, 9, AMOUNT_RUPEE_X, AMOUNT_RIGHT_X, y, fmtNumber(v));
        return y - 14;
    }

    private void drawLeaderLine(
            PDPageContentStream cs,
            PDFont font,
            int fontSize,
            float x,
            float y,
            String text,
            float endX
    ) throws Exception {
        if (text == null) return;

        int es = effectiveFontSize(fontSize);
        float startX;
        try {
            float tw = font.getStringWidth(text) / 1000 * es;
            startX = x + tw + 4;
        } catch (Exception ex) {
            return;
        }

        // Only draw if there's visible space.
        if (endX <= startX + 12) return;

        cs.saveGraphicsState();
        cs.setStrokingColor(0f, 0f, 0f);
        cs.setLineWidth(0.3f);
        // Slightly below baseline so it reads like a guide.
        float ly = y + 2;
        cs.moveTo(startX, ly);
        cs.lineTo(endX, ly);
        cs.stroke();
        cs.restoreGraphicsState();
    }

    private String currencyMark(Fonts fonts) {
        return fonts.supportsRupee() ? "₹" : "Rs.";
    }

    private void drawCurrencyAmountRight(
            PDPageContentStream cs,
            Fonts fonts,
            PDFont f,
            int s,
            float rupeeX,
            float numberRightX,
            float y,
            String numberOrNil
    ) throws Exception {
        if (numberOrNil == null) numberOrNil = "";
        String trimmed = numberOrNil.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("NIL")) {
            textRight(cs, f, s, numberRightX, y, trimmed.isEmpty() ? "NIL" : trimmed);
            return;
        }

        // Currency symbol column (fixed x), number column (right-aligned).
        String symbol = fonts.supportsRupee() ? "₹" : "Rs.";
        text(cs, f, s, rupeeX, y, symbol);
        textRight(cs, f, s, numberRightX, y, trimmed);
    }

    private void textRight(PDPageContentStream cs, PDFont f, int s, float rightX, float y, String t) throws Exception {
        if (t == null) t = "";
        int es = effectiveFontSize(s);
        float tw;
        try {
            tw = f.getStringWidth(t) / 1000 * es;
        } catch (Exception ex) {
            // Fallback: draw at a safe left offset if width can't be measured.
            tw = 0;
        }
        float startX = rightX - tw;
        textRaw(cs, f, es, startX, y, t);
    }

    private String fmtNumber(double v) {
        return String.format("%,.2f", v);
    }

    private void text(PDPageContentStream cs, PDFont f, int s, float x, float y, String t) throws Exception {
        int es = effectiveFontSize(s);
        textRaw(cs, f, es, x, y, t);
    }

    private void center(PDPageContentStream cs, PDFont f, int s, float w, float y, String t) throws Exception {
        int es = effectiveFontSize(s);
        float tw = f.getStringWidth(t) / 1000 * es;
        textRaw(cs, f, es, (w - tw) / 2, y, t);
    }

    private Fonts loadFonts(PDDocument doc) throws Exception {
        // Prefer Unicode fonts so ₹ is available.
        File[][] candidates = {
                {new File("C:\\Windows\\Fonts\\arial.ttf"), new File("C:\\Windows\\Fonts\\arialbd.ttf")},
                {new File("C:\\Windows\\Fonts\\segoeui.ttf"), new File("C:\\Windows\\Fonts\\segoeuib.ttf")},
                {new File("C:\\Windows\\Fonts\\Nirmala.ttf"), new File("C:\\Windows\\Fonts\\NirmalaB.ttf")}
        };

        for (File[] pair : candidates) {
            if (pair[0].exists() && pair[1].exists()) {
                PDFont normal = PDType0Font.load(doc, pair[0]);
                PDFont bold = PDType0Font.load(doc, pair[1]);
                boolean rupeeOk = canShowText(normal, "₹") && canShowText(bold, "₹");
                return new Fonts(normal, bold, rupeeOk);
            }
        }

        // Fallback: standard 14 fonts (Helvetica does NOT support ₹)
        return new Fonts(new PDType1Font(FontName.HELVETICA), new PDType1Font(FontName.HELVETICA_BOLD), false);
    }

    private boolean canShowText(PDFont font, String text) {
        try {
            font.getStringWidth(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}