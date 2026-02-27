package pks.IncomeTax.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pks.IncomeTax.model.Employee;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class ReportService {

    public String generateExcelReport(Employee emp) throws Exception {
        Workbook wb = new XSSFWorkbook();
        try {
            Sheet sheet = wb.createSheet("Income Tax Report");

            // Simple column widths to make layout look better
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 6000);
            sheet.setColumnWidth(2, 6000);
            sheet.setColumnWidth(3, 6000);

            // Create a bold style for headings
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle heading = wb.createCellStyle();
            heading.setFont(bold);

            int rownum = 0;

            // Title
            Row r = sheet.createRow(rownum++);
            Cell c = r.createCell(0);
            c.setCellValue("(क) वेतन स्रोत से आय का विवरण");
            c.setCellStyle(heading);

            rownum++;

            // Basic personal info rows (approximate layout)
            Row r1 = sheet.createRow(rownum++);
            r1.createCell(0).setCellValue("नाम:");
            r1.createCell(1).setCellValue(emp.getName() != null ? emp.getName() : "");

            Row r2 = sheet.createRow(rownum++);
            r2.createCell(0).setCellValue("कर्मचारी आईडी:");
            r2.createCell(1).setCellValue(emp.getEmployeeId() != null ? emp.getEmployeeId() : "");

            Row r3 = sheet.createRow(rownum++);
            r3.createCell(0).setCellValue("पद/विभाग:");
            r3.createCell(1).setCellValue((emp.getPost() != null ? emp.getPost() : "") + " / " + (emp.getDepartment() != null ? emp.getDepartment() : ""));

            Row r4 = sheet.createRow(rownum++);
            r4.createCell(0).setCellValue("वित्तीय वर्ष:");
            r4.createCell(1).setCellValue(emp.getFinancialYear() != null ? emp.getFinancialYear() : "");

            rownum++;

            // Salary details table header
            Row h = sheet.createRow(rownum++);
            h.createCell(0).setCellValue("वेतन मद");
            h.getCell(0).setCellStyle(heading);
            h.createCell(1).setCellValue("राशि (Rs)");
            h.getCell(1).setCellStyle(heading);

            // Salary rows
            Row s1 = sheet.createRow(rownum++);
            s1.createCell(0).setCellValue("मूल वेतन:");
            s1.createCell(1).setCellValue(emp.getBasicPay() != null ? emp.getBasicPay().doubleValue() : 0.0);

            Row s2 = sheet.createRow(rownum++);
            s2.createCell(0).setCellValue("DA:");
            s2.createCell(1).setCellValue(emp.getDa() != null ? emp.getDa().doubleValue() : 0.0);

            Row s3 = sheet.createRow(rownum++);
            s3.createCell(0).setCellValue("TA:");
            s3.createCell(1).setCellValue(emp.getTa() != null ? emp.getTa().doubleValue() : 0.0);

            Row s4 = sheet.createRow(rownum++);
            s4.createCell(0).setCellValue("HRA:");
            s4.createCell(1).setCellValue(emp.getHra() != null ? emp.getHra().doubleValue() : 0.0);

            Row s5 = sheet.createRow(rownum++);
            s5.createCell(0).setCellValue("Medical Allowances:");
            s5.createCell(1).setCellValue(emp.getMedicalAllowances() != null ? emp.getMedicalAllowances().doubleValue() : 0.0);

            // Total
            double total = (emp.getBasicPay() != null ? emp.getBasicPay().doubleValue() : 0.0)
                    + (emp.getDa() != null ? emp.getDa().doubleValue() : 0.0)
                    + (emp.getTa() != null ? emp.getTa().doubleValue() : 0.0)
                    + (emp.getHra() != null ? emp.getHra().doubleValue() : 0.0)
                    + (emp.getMedicalAllowances() != null ? emp.getMedicalAllowances().doubleValue() : 0.0);

            Row totalRow = sheet.createRow(rownum++);
            totalRow.createCell(0).setCellValue("कुल वेतन:");
            totalRow.createCell(1).setCellValue(total);

            // Footer with place and date
            rownum++;
            Row foot = sheet.createRow(rownum++);
            foot.createCell(0).setCellValue("Department: " + (emp.getDepartment() != null ? emp.getDepartment() : ""));
            foot.createCell(2).setCellValue("Date: " + Instant.now().toString().substring(0,10));

            // Ensure reports directory exists
            Path reportsDir = Path.of("reports");
            if (!Files.exists(reportsDir)) Files.createDirectories(reportsDir);

            String filename = String.format("income-report-%d-%d.xlsx", emp.getId() != null ? emp.getId() : 0, Instant.now().toEpochMilli());
            File out = reportsDir.resolve(filename).toFile();
            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
            return filename;
        } finally {
            wb.close();
        }
    }
}
