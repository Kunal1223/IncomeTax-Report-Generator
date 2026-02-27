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

    private record Fonts(PDFont normal, PDFont bold, boolean supportsRupee) {
    }

    public String generate(Employee e) throws Exception {

        Path dir = Path.of("reports");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        File file = dir.resolve("Schedule_of_Income_Tax.pdf").toFile();

        try (PDDocument doc = new PDDocument()) {
            Fonts fonts = loadFonts(doc);
            PDPage page = new PDPage(PDRectangle.A4);
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
                center(cs, fonts.bold(), 12, pw, ph - 50, "Schedule of Income Tax");
                center(cs, fonts.normal(), 9, pw, ph - 65, "(Fill in four copies)");
                center(cs, fonts.normal(), 9, pw, ph - 80,
                        "Financial Year 2025-2026 (Assessment Year 2026-2027)");

                float y = ph - 110;
                float lx = 40;
                float rx = 430;

                /* ---------- BASIC DETAILS ---------- */
                y = labelValue(cs, fonts, y, lx, 260, "Name of the Employee:", e.getName());
                y = labelValue(cs, fonts, y, lx, 260, "Designation:", "Software Engineer");
                y = labelValue(cs, fonts, y, lx, 260, "Permanent Account Number (PAN):", e.getPan());
                y = labelValue(cs, fonts, y, lx, 260, "Office Name:", "Income Tax Department");
                y = labelValue(cs, fonts, y, lx, 260, "Office TAN:", "ABCDE1234F");
                y = labelValue(cs, fonts, y, lx, 260, "Treasury Name:", "Central Treasury");

                /* ---------- SECTION A ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(A) Income from Salary :");
                y -= 20;

                y = money(cs, fonts, y, "(1) Basic Salary (01-03-2025 to 28-02-2026):", 3423423, false);
                y = money(cs, fonts, y, "(2) Dearness Allowance:", 123456, false);
                y = money(cs, fonts, y, "(3) House Rent Allowance:", 234567, false);
                y = money(cs, fonts, y, "(4) Medical Allowance:", 12345, false);
                y = money(cs, fonts, y, "(5) Transport Allowance:", 123456, false);
                y = money(cs, fonts, y, "(6) DA on Transport Allowance:", 12345, false);
                y = money(cs, fonts, y, "(7) Special Pay/Bonus/Honorarium/Nursing Allowance/Other Allowances:", 123456, false);
                y = money(cs, fonts, y, "(8) Arrear of Dearness Allowance:", 1234567, false);
                y = money(cs, fonts, y, "(9) Arrear of Pay and Allowances:", 1234567890L, false);
                y = money(cs, fonts, y, "(10) Total Income from Salary:", 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L, true);
                y = money(cs, fonts, y, "(-) Less : Standard deduction u/s 16(1)", 123456789L, false);
                y = money(cs, fonts, y, "Total Income from Salary", 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L - 123456789L, true);

                /* ---------- SECTION B ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(B) Income from House Property :");
                y -= 20;

                y = money(cs, fonts, y, "(i) Income from House Rent", 123456789L, false);
                y = money(cs, fonts, y, "(ii) Interest on Housing Loan (u/s 24b)", 12345678L, false);
                y = money(cs, fonts, y, "Total Income from House Property", 123456789L + 12345678L, true);

                /* ---------- SECTION C ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "(C) Income from Other Sources :");
                y -= 20;

                y = money(cs, fonts, y, "(i) Interest on Saving A/c of Bank/Post Office", 12345678L, false);
                y = money(cs, fonts, y, "(ii) Interest on Fixed Deposit / Recurring Deposit / KVP etc.", 123456789L, false);
                y = money(cs, fonts, y, "(iii) Any other Income / Commission / etc.", 1234567890L, false);
                y = money(cs, fonts, y, "Total Income from Other Sources", 12345678L + 123456789L + 1234567890L, true);

                /* ---------- GROSS TOTAL ---------- */
                y -= 6;
                blackHeader(cs, fonts, lx, y, 520, "GROSS TOTAL INCOME");
                y -= 22;
                y = money(cs, fonts, y, "GROSS TOTAL INCOME (ROUNDED OFF UPTO Rs. 10/-)", 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L + 1234567890L - 123456789L + 123456789L + 12345678L + 12345678L + 123456789L + 1234567890L, true);

                /* ---------- TAX TABLE ---------- */
                y -= 10;
                blackHeader(cs, fonts, lx, y, 520, "CALCULATION OF INCOME TAX PAYABLE");
                y -= 18;
                y = drawTaxTable(cs, fonts, lx, y, e);

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
                y = money(cs, fonts, y, "Net Income Tax Payable", 123456789L, true);
                y = money(cs, fonts, y, "Add : 4% Health and Education Cess on Rs.", 12345678L, false);
                y = money(cs, fonts, y, "Total Income Tax and Health & Education Cess Payable", 123456789L + 12345678L, true);
                y = money(cs, fonts, y, "Less: Income Tax paid / deducted monthly from salary (-)", 123456789L, false);
                y = money(cs, fonts, y,
                        "Balance: Income Tax deposited / deducted through Salary for the month of February",
                        123456789L, false);
                y = money(cs, fonts, y,
                        "Payable Income Tax and Health & Education Cess for Financial Year 2025-26",
                        123456789L, true);

                /* ---------- FOOTER ---------- */
                text(cs, fonts.normal(), 9, lx, 70, "Place: " + 534534534);
                text(cs, fonts.normal(), 9, lx, 55, "Date : " + LocalDate.now());

                text(cs, fonts.normal(), 9, pw - 240, 70, "Signature and Seal");
                text(cs, fonts.normal(), 9, pw - 260, 55, "Drawing & Disbursing Officer");
                text(cs, fonts.normal(), 9, lx, 40, "Taxpayer's Signature");
            }
            doc.save(file);
        }
        return file.getName();
    }

    /* ===================== TAX TABLE ===================== */

    private float drawTaxTable(PDPageContentStream cs, Fonts fonts, float x, float y, Employee e) throws Exception {

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

        String[][] data = {
                {"(i) First", "Rs. 1 to Rs. 4,00,000", "NIL", "NIL"},
            {"(ii) Next", "Rs. 4,00,001 to Rs. 8,00,000", "5.00%", fmt(fonts, 123456789L)},
            {"(iii) Next", "Rs. 8,00,001 to Rs. 12,00,000", "10.00%", fmt(fonts, 123456789L)},
            {"(iv) Next", "Rs. 12,00,001 to Rs. 16,00,000", "15.00%", fmt(fonts, 123456789L)},
            {"(v) Next", "Rs. 16,00,001 to Rs. 20,00,000", "20.00%", fmt(fonts, 123456789L)},
            {"(vi) Next", "Rs. 20,00,001 to Rs. 24,00,000", "25.00%", fmt(fonts, 123456789L)},
            {"(vii) Balance", "Rs. 24,00,001 to above", "30.00%", fmt(fonts, 123456789)},
            {"", "", "Total :", fmt(fonts, 123456789L + 123456789L + 123456789L + 123456789L + 123456789L + 123456789L + 123456789L)}
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

    /* ===================== HELPERS ===================== */

    private void blackHeader(PDPageContentStream cs, Fonts fonts, float x, float y, float w, String text) throws Exception {
        float h = 14;
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.addRect(x, y - h, w, h);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(fonts.bold(), 9);
        cs.newLineAtOffset(x + 4, y - h / 2 + 3);
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