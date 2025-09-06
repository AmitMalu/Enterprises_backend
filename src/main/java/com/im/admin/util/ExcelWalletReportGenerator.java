package com.im.admin.util;


import com.im.admin.dto.WalletReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelWalletReportGenerator {
    public static void generateWalletExcel(List<WalletReportDto> data, File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Wallets");
            int row = 0;
            Row header = sheet.createRow(row++);
            header.createCell(0).setCellValue("Transaction ID");
            header.createCell(1).setCellValue("Hierarchy ID");
            header.createCell(2).setCellValue("Wallet Type");
            header.createCell(3).setCellValue("Amount");
            header.createCell(4).setCellValue("Balance Before");
            header.createCell(5).setCellValue("Balance After");
            header.createCell(6).setCellValue("Status");
            header.createCell(7).setCellValue("Date");
            header.createCell(8).setCellValue("Time");

            for (WalletReportDto dto : data) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(dto.getTransactionId() == null ? "" : dto.getTransactionId());
                r.createCell(1).setCellValue(dto.getHierarchyId() == null ? "" : dto.getHierarchyId());
                r.createCell(2).setCellValue(dto.getWalletType() == null ? "" : dto.getWalletType());
                r.createCell(3).setCellValue(dto.getAmount() == null ? 0.0 : dto.getAmount().doubleValue());
                r.createCell(4).setCellValue(dto.getBalBefore() == null ? 0.0 : dto.getBalBefore().doubleValue());
                r.createCell(5).setCellValue(dto.getBalAfter() == null ? 0.0 : dto.getBalAfter().doubleValue());
                r.createCell(6).setCellValue(dto.getStatus() == null ? "" : dto.getStatus());
                r.createCell(7).setCellValue(dto.getDate() == null ? "" : dto.getDate().toString());
                r.createCell(8).setCellValue(dto.getTime() == null ? "" : dto.getTime().toString());
            }
            for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
}

