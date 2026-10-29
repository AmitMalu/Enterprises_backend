package com.scs.admin.service.impl;

import com.scs.admin.entities.CardTypeMaster;
import com.scs.admin.repositories.CardTypeMasterRepository;
import com.scs.admin.service.CardTypeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class CardTypeServiceImpl implements CardTypeService {

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;


    @Override
    public ByteArrayResource generateSampleTemplate() throws IOException {
        log.info("Generating sample template for card type");

        List<String> headers = List.of("Bin No", "Entry", "Network", "Type", "Sub Type", "Issuer Code", "Issuer Name");
        List<String> rowData = List.of("428094", "iin", "Visa", "debit", "consumer", "KKBK", "Kotak Mahindra Bank");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Card Type");

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            log.info("Header row created: {}", headers);

            // Create data row
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < rowData.size(); i++) {
                dataRow.createCell(i).setCellValue(rowData.get(i));
            }
            log.info("Data row created: {}", rowData);

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Sample template generated successfully, size={} bytes", out.size());

            return new ByteArrayResource(out.toByteArray());
        }
    }


    public String uploadCardTypes(MultipartFile file) throws IOException {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            return "Invalid file. Only .xlsx supported.";
        }

        List<String> expectedHeaders = List.of(
                "Bin No", "Entry", "Network", "Type", "Sub Type", "Issuer Code", "Issuer Name"
        );

        int success = 0, skipped = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (!isHeaderValid(headerRow, expectedHeaders)) {
                throw new IllegalArgumentException("Excel headers do not match the expected format. "
                        + "Expected: " + expectedHeaders);
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String bin = getCellString(row, 0);
                if (bin == null || bin.isBlank()) continue;

                if (cardTypeMasterRepository.existsByCardBinNo(bin)) {
                    skipped++;
                    continue;
                }

                CardTypeMaster card = new CardTypeMaster();
                card.setCardBinNo(bin);
                card.setEntry(getCellString(row, 1));
                card.setNetwork(getCellString(row, 2));
                card.setCardType(getCellString(row, 3));
                card.setSubType(getCellString(row, 4));
                card.setIssuerCode(getCellString(row, 5));
                card.setIssuerName(getCellString(row, 6));
                card.setIsBlocked(false);

                cardTypeMasterRepository.save(card);
                success++;
            }
        }

        return "Upload completed. Inserted: " + success + ", Skipped (duplicate BINs): " + skipped;
    }

    private boolean isHeaderValid(Row headerRow, List<String> expectedHeaders) {
        if (headerRow == null || headerRow.getPhysicalNumberOfCells() != expectedHeaders.size()) {
            return false;
        }

        for (int i = 0; i < expectedHeaders.size(); i++) {
            String actual = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    .getStringCellValue().trim();
            if (!expectedHeaders.get(i).equalsIgnoreCase(actual)) {
                return false;
            }
        }

        return true;
    }


    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}
