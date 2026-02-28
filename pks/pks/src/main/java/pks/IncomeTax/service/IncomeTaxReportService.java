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

    private record Fonts(PDFont normal, PDFont bold, boolean supportsRupee) {
    }

    private record TaxSummary(double taxBeforeRebate, double rebate87A, double netTax, double cess, double totalPayable) {
    }

    private long safe(Long v) {
        return v == null ? 0L : v.longValue();
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
                    PDRectangle.A4.getWidth(),
                    PDRectangle.A4.getHeight() + EXTRA_PAGE_HEIGHT
            );
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float pw = page.getMediaBox().getWidth();
                float ph = page.getMediaBox().getHeight();

                /* ---------- OUTER BORDER ---------- */
                cs.setLineDashPattern(new float[]{4, 3}, 0);
                cs.addRect(20, 20, pw - 40, ph - 40);
                cs.stroke();
                cs.setLineDashPattern(new float[]{}, 0);

                /* ---------- HEADER ---------- */
                center(cs, fonts.bold(), 12, pw, ph - 60, "Schedule of Income Tax");
                center(cs, fonts.normal(), 9, pw, ph - 70, "(Fill in four copies)");
                center(cs, fonts.normal(), 9, pw, ph - 86, buildYearHeader(e));

                float y = ph - 110;
                float lx = 40;
                float rx = 430;

                /* ---------- BASIC DETAILS ---------- */
                y = labelValue(cs, fonts, y, lx, 260, "Name of the Employee:", e.getName() != null ? e.getName() : "");
                y = labelValue(cs, fonts, y, lx, 260, "Designation:", e.getPost() != null ? e.getPost() : "");
                y = labelValue(cs, fonts, y, lx, 260, "Permanent Account Number (PAN):", e.getPan() != null ? e.getPan() : "");
                // As per UI mapping: Office Name = department, Office TAN = employerTan, Treasury Name = treasuryName
                y = labelValue(cs, fonts, y, lx, 260, "Office Name:", e.getDepartment() != null ? e.getDepartment() : "");
                y = labelValue(cs, fonts, y, lx, 260, "Office TAN:", e.getEmployerTan() != null ? e.getEmployerTan() : "");
                y = labelValue(cs, fonts, y, lx, 260, "Treasury Name:", e.getTreasuryName() != null ? e.getTreasuryName() : "");

                /* ---------- SECTION A ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(A) Income from Salary :");
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
                y = money(cs, fonts, y, "(10) Total Income from Salary:", totalIncomeFromSalary, true);

                long standardDeduction = 0L;
                y = money(cs, fonts, y, "(-) Less : Standard deduction u/s 16(1)", standardDeduction, false);
                y = money(cs, fonts, y, "Total Income from Salary", totalIncomeFromSalary - standardDeduction, true);

                /* ---------- SECTION B ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(B) Income from House Property :");
                y -= (14 + AFTER_HEADER_GAP);

                long houseRentIncome = safe(e.getIncomeFromHouseRent());
                long housingLoanInterest = safe(e.getInterestOnHousingLoan());
                long totalHouseProperty = houseRentIncome + housingLoanInterest;
                y = money(cs, fonts, y, "(i) Income from House Rent", houseRentIncome, false);
                y = money(cs, fonts, y, "(ii) Interest on Housing Loan (u/s 24b)", housingLoanInterest, false);
                y = money(cs, fonts, y, "Total Income from House Property", totalHouseProperty, true);

                /* ---------- SECTION C ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(C) Income from Other Sources :");
                y -= (14 + AFTER_HEADER_GAP);

                long interestSaving = safe(e.getInterestOnSaving());
                long interestFD = safe(e.getInterestOnFixedDeposit());
                long otherIncome = safe(e.getAnyOtherIncome());
                long totalOtherSources = interestSaving + interestFD + otherIncome;
                y = money(cs, fonts, y, "(i) Interest on Saving A/c of Bank/Post Office", interestSaving, false);
                y = money(cs, fonts, y, "(ii) Interest on Fixed Deposit / Recurring Deposit / KVP etc.", interestFD, false);
                y = money(cs, fonts, y, "(iii) Any other Income / Commission / etc.", otherIncome, false);
                y = money(cs, fonts, y, "Total Income from Other Sources", totalOtherSources, true);

                /* ---------- GROSS TOTAL ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "GROSS TOTAL INCOME");
                y -= (14 + AFTER_HEADER_GAP);
                long grossTotalIncome = totalIncomeFromSalary + totalHouseProperty + totalOtherSources; // salary + house + other
                y = money(cs, fonts, y, "GROSS TOTAL INCOME (ROUNDED OFF UPTO Rs. 10/-)", grossTotalIncome, true);

                /* ---------- TAX TABLE ---------- */
                y -= 10;
                blackHeader(cs, fonts, lx, y, 520, "CALCULATION OF INCOME TAX PAYABLE");
                y -= (14 + AFTER_HEADER_GAP);
                y = drawTaxTable(cs, fonts, lx, y, grossTotalIncome);

                /* ---------- FOOT NOTES ---------- */
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                        "* Tax-free Income: Basic exemption limit is Rs 4,00,000 for male/female taxpayers below 60 years of age.");
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                        "Less: Tax Relief under Section 87A under New Tax Regime (Rebate up to Rs 60,000 for total income up to Rs 12 lakhs).");
                y -= 12;
                text(cs, fonts.normal(), 8, lx, y,
                        "If total income exceeds Rs 12 lakhs, Marginal Relief will be granted.");

                /* ---------- FINAL TOTALS ---------- */
                y -= 18;
                TaxSummary tax = computeTaxSummary(grossTotalIncome);
                y = money(cs, fonts, y, "Net Income Tax Payable", tax.netTax(), true);
                y = money(cs, fonts, y, "Add : 4% Health and Education Cess on Rs.", tax.cess(), false);
                y = money(cs, fonts, y, "Total Income Tax and Health & Education Cess Payable", tax.totalPayable(), true);
                y = money(cs, fonts, y, "Less: Income Tax paid / deducted monthly from salary (-)", 0L, false);
                y = money(cs, fonts, y,
                    "Balance: Income Tax deposited / deducted through Salary for the month of February",
                    0L, false);
                String fyShort = buildFinancialYearShort(e);
                y = money(cs, fonts, y,
                    "Payable Income Tax and Health & Education Cess for Financial Year " + fyShort,
                    tax.totalPayable(), true);

                /* ---------- FOOTER ---------- */
                // Place: use department if available
                String place = e.getDepartment() != null ? e.getDepartment() : "";
                text(cs, fonts.normal(), 9, lx, 70, "Place: " + "Bettiah");
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
        float tableW = 520;
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

        String[][] data = {
                {"(i) First", "Rs. 1 to Rs. 4,00,000", "NIL", "NIL"},
                {"(ii) Next", "Rs. 4,00,001 to Rs. 8,00,000", "5.00%", slabTax[1] <= 0 ? "NIL" : fmt(fonts, slabTax[1])},
                {"(iii) Next", "Rs. 8,00,001 to Rs. 12,00,000", "10.00%", slabTax[2] <= 0 ? "NIL" : fmt(fonts, slabTax[2])},
                {"(iv) Next", "Rs. 12,00,001 to Rs. 16,00,000", "15.00%", slabTax[3] <= 0 ? "NIL" : fmt(fonts, slabTax[3])},
                {"(v) Next", "Rs. 16,00,001 to Rs. 20,00,000", "20.00%", slabTax[4] <= 0 ? "NIL" : fmt(fonts, slabTax[4])},
                {"(vi) Next", "Rs. 20,00,001 to Rs. 24,00,000", "25.00%", slabTax[5] <= 0 ? "NIL" : fmt(fonts, slabTax[5])},
                {"(vii) Balance", "Rs. 24,00,001 to above", "30.00%", slabTax[6] <= 0 ? "NIL" : fmt(fonts, slabTax[6])},
                {"", "", "Total :", totalTax <= 0 ? "NIL" : fmt(fonts, totalTax)}
        };

        float ty = y - 12;
        for (String[] r : data) {
            text(cs, fonts.normal(), 9, cols[0] + 2, ty, r[0]);
            text(cs, fonts.normal(), 9, cols[1] + 2, ty, r[1]);
            text(cs, fonts.normal(), 9, cols[2] + 2, ty, r[2]);
            text(cs, fonts.bold(), 9, cols[3] + 6, ty, r[3]);
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

    private void blackHeader(PDPageContentStream cs, Fonts fonts,
                         float x, float y, float w, String text) throws Exception {

        float h = 14f;
        float fontSize = 9f;
        float bottomPadding = 2f; // space below text

        /* ---- Draw black background ---- */
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.addRect(x, y - h, w, h);
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

    private float labelValue(PDPageContentStream cs, Fonts fonts, float y, float lx, float vx, String l, String v) throws Exception {
        text(cs, fonts.normal(), 9, lx, y, l);
        text(cs, fonts.bold(), 9, vx, y, v == null ? "" : v);
        return y - 14;
    }

    private float money(PDPageContentStream cs, Fonts fonts, float y, String l, double v, boolean bold) throws Exception {
        text(cs, fonts.normal(), 9, 40, y, l);
        text(cs, bold ? fonts.bold() : fonts.normal(), 9, 430, y, fmt(fonts, v));
        return y - 14;
    }

    private void text(PDPageContentStream cs, PDFont f, int s, float x, float y, String t) throws Exception {
        cs.beginText();
        cs.setFont(f, s);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private void center(PDPageContentStream cs, PDFont f, int s, float w, float y, String t) throws Exception {
        float tw = f.getStringWidth(t) / 1000 * s;
        text(cs, f, s, (w - tw) / 2, y, t);
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

    private String fmt(Fonts fonts, double v) {
        if (fonts.supportsRupee()) {
            return String.format("₹ %,.2f", v);
        }
        return String.format("Rs. %,.2f", v);
    }
}