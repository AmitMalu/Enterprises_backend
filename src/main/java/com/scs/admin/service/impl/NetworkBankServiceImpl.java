package com.scs.admin.service.impl;

import com.scs.admin.dto.BankListRequest;
import com.scs.admin.dto.BankRequestDto;
import com.scs.admin.entities.NetworkBankMaster;
import com.scs.admin.repositories.NetworkBankMasterRepository;
import com.scs.admin.request.NetworkBankMasterResponseDto;
import com.scs.admin.response.BankInfoResponse;
import com.scs.admin.response.NetworkBankDropdownResponse;
import com.scs.admin.service.BankBulkUploadResult;
import com.scs.admin.service.NetworkBankService;
import com.scs.admin.spec.BankSpecification;
import com.scs.admin.util.ImageUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@Service
public class NetworkBankServiceImpl implements NetworkBankService {

    @Autowired
    private NetworkBankMasterRepository bankMasterRepository;

    @Autowired
    private ImageUtil imageUtil;


    @Override
    public NetworkBankMaster addBank(BankRequestDto dto, MultipartFile logoFile) throws IOException {
        if (dto.getBankCode() == null || dto.getBankName() == null) {
            throw new IllegalArgumentException("Bank Code and Bank Name are required.");
        }

        NetworkBankMaster bank = getNetworkBankMaster(dto);

        try {
            if (logoFile != null && !logoFile.isEmpty()) {
                String imagePath = imageUtil.saveImage(logoFile, "Bank_Logos");
                log.info("Saved logo path: {}", imagePath);
                bank.setLogo(imagePath);
            } else {
                log.warn("Logo file is null or empty!");
            }

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        return bankMasterRepository.save(bank);
    }

    private static NetworkBankMaster getNetworkBankMaster(BankRequestDto dto) {
        NetworkBankMaster bank = new NetworkBankMaster();
        bank.setBankCode(dto.getBankCode());
        bank.setBankCodeBI(dto.getBankCodeBi());
        bank.setBankName(dto.getBankName());
        //bank.setBankBranchName(dto.getBankBranchName());
        bank.setIfscCode(dto.getIfscCode());
        bank.setImps(dto.getImps());
        bank.setNeft(dto.getNeft());
        //bank.setRtgs(dto.getRtgs());
        bank.setIsActive(dto.getIsActive());
        bank.setCreatedBy(dto.getCreatedBy());
        bank.setEditedBy(dto.getEditedBy());
        bank.setIsBlocked(dto.getIsBlocked());
        bank.setMaxAccountNoLength(dto.getMaxAccountNoLength());
        bank.setLogo(dto.getLogo());
        return bank;
    }


    @Override
    public Page<NetworkBankMaster> getAllBanks(
            Pageable pageable,
            String id,
            String bankCode,
            String bankName,
            String bankBranchName,
            String ifscCode,
            Boolean imps,
            Boolean neft,
            Boolean rtgs,
            Boolean isActive,
            String createdBy,
            String editedBy,
            Boolean isBlocked,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        Specification<NetworkBankMaster> spec = BankSpecification.getFilter(
                id, bankCode, bankName, bankBranchName, ifscCode,
                imps, neft, rtgs, isActive,
                createdBy, editedBy, isBlocked, createdOn, editedOn
        );

        return bankMasterRepository.findAll(spec, pageable);
    }


    @Override
    public NetworkBankMaster updateBank(Long id, BankRequestDto dto, MultipartFile logoFile) {
        log.info("Updating BankMaster with ID: {}", id);

        NetworkBankMaster existing = bankMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found with ID: " + id));

        existing.setBankCode(dto.getBankCode());
        existing.setBankCodeBI(dto.getBankCodeBi());
        existing.setBankName(dto.getBankName());
        //existing.setBankBranchName(dto.getBankBranchName());
        existing.setIfscCode(dto.getIfscCode());
        existing.setImps(dto.getImps());
        existing.setNeft(dto.getNeft());
        //existing.setRtgs(dto.getRtgs());
        existing.setIsActive(dto.getIsActive());
        existing.setIsBlocked(dto.getIsBlocked());
        existing.setEditedBy(dto.getEditedBy());
        existing.setEditedOn(LocalDateTime.now());
        existing.setMaxAccountNoLength(dto.getMaxAccountNoLength());
        existing.setBankCodeBI(dto.getBankCodeBi());

        // Optional: handle logo re-upload
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String imagePath = imageUtil.saveImage(logoFile, "Bank_Logos");
                existing.setLogo(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Logo upload failed", e);
            }
        }

        return bankMasterRepository.save(existing);
    }


    @Override
    public NetworkBankMaster getBankById(Long id) {
        log.info("Fetching bank with ID: {}", id);
        return bankMasterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found with ID: " + id));
    }

    public List<NetworkBankDropdownResponse> getBankDropdownList() {
        return bankMasterRepository.getActiveBankDropdown();
    }

    @Override
    public BankInfoResponse getBankInfoByIfsc(String ifscCode) {

        String trimmedIfsc = ifscCode.trim();

        NetworkBankMaster bank = bankMasterRepository
                .findByIfscCodeIgnoreCase(trimmedIfsc)
                .or(() -> bankMasterRepository.findByUnisusMIfcsCodeIgnoreCase(trimmedIfsc))
                .orElseThrow(() -> new NoSuchElementException("No bank found with IFSC: " + ifscCode));

        return new BankInfoResponse(bank.getBankName());
    }

    @Override
    public List<NetworkBankMasterResponseDto> getAllNetworkBanks() {

        return bankMasterRepository.findAll()
                .stream()
                .filter(bank -> bank.getId() != null)
                .map(bank -> new NetworkBankMasterResponseDto(
                        bank.getId(),
                        bank.getBankName(),
                        bank.getIfscCode()
                ))
                .toList();
    }


    @Override
    public void saveBankList(BankListRequest request) {

        List<NetworkBankMaster> banks = request.getBankList()
                .stream()
                .map(dto -> {

                    NetworkBankMaster bank = new NetworkBankMaster();

                    bank.setUnisusBankId(Integer.parseInt(dto.getBankId()));
                    bank.setBankCode(dto.getBankCode());
                    bank.setBankName(dto.getBankName());
                    bank.setUnisusMIfcsCode(dto.getMifscCode());

                    bank.setImps("1".equals(dto.getImps()));
                    bank.setNeft("1".equals(dto.getNeft()));
                    bank.setIsActive("1".equals(dto.getStatus()));

                    bank.setCreatedBy("SYSTEM");

                    return bank;

                }).toList();

        bankMasterRepository.saveAll(banks);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK UPLOAD
    // ─────────────────────────────────────────────────────────────────────────

    private static final int BULK_BATCH_SIZE = 500;

    /**
     * Reads the Excel file row-by-row (streaming — never loads all into a List),
     * builds NetworkBankMaster entities, and flushes them in batches of 500
     * using saveAll() backed by JDBC batch inserts.
     *
     * Expected column order (0-indexed, header row skipped):
     *   0  Bank Code
     *   1  Bank Code BI
     *   2  Bank Name          ← mandatory
     *   3  Vidual Bank Code
     *   4  IMPS               (Y/N/y/n/true/false/1/0)
     *   5  NEFT               (same)
     *   6  Logo               (URL or blank)
     *   7  IFSC Code
     *   8  Max Account No Length
     *   9  Verification       (Y/S = true)
     *   10 Active Status      (Y/S = true)
     *   11 Blocked            (true/false/Y/N)
     */

    @Override
    public BankBulkUploadResult bulkUpload(
            MultipartFile file,
            String createdBy
    ) {
        log.info(
                "Bank upload started file={} createdBy={}",
                file.getOriginalFilename(),
                createdBy
        );

        long startMs = System.currentTimeMillis();

        int totalRows = 0;
        int inserted = 0;
        int errorCount = 0;

        List<String> errors = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();

        try (
                InputStream input = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(input)
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            List<NetworkBankMaster> batch = new ArrayList<>(BULK_BATCH_SIZE);

            // skip header
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                totalRows++;

                try {
                    NetworkBankMaster bank = new NetworkBankMaster();

                    // CORRECTED COLUMN MAPPING based on your Excel structure
                    // Column 0: Bank Code (UN, IBKL, UNB, etc.)
                    bank.setBankCode(getString(formatter, row, 0));

                    // Column 1: Bank Code BI (328, 518, 501, etc.)
                    bank.setBankCodeBI(getString(formatter, row, 1));

                    // Column 2: Bank Name
                    bank.setBankName(getString(formatter, row, 2));

                    // Column 3: Vidual Bank Code (Y, N, or numeric like 004)
                    // This field contains either Y/N or numeric values
                    String vidualBankCode = getString(formatter, row, 3);
                    if (vidualBankCode != null && !vidualBankCode.isBlank()) {
                        // Try to parse as integer, if fails, store as is or null
                        try {
                            bank.setVidualBankCode(Integer.valueOf(vidualBankCode.trim()));
                        } catch (NumberFormatException e) {
                            // If it's Y/N or other text, set as null
                            bank.setVidualBankCode(null);
                        }
                    }

                    // Column 4: MPS (Y/N)
                    bank.setImps(getBoolean(formatter, row, 4));

                    // Column 5: NETF (Y/N)
                    bank.setNeft(getBoolean(formatter, row, 5));

                    // Column 6: Legal (Y/N) - Note: This is Legal in Excel but your code uses Logo
                    // If you have a setLegal() method, use that, otherwise setLogo
                    bank.setLogo(getString(formatter, row, 6));

                    // Column 7: IFSC Code
                    bank.setIfscCode(getString(formatter, row, 7));

                    // Column 8: Max Account No Length (Y/N or numeric)
                    String maxAccountLength = getString(formatter, row, 8);
                    if (maxAccountLength != null && !maxAccountLength.isBlank()) {
                        try {
                            bank.setMaxAccountNoLength(Integer.valueOf(maxAccountLength.trim()));
                        } catch (NumberFormatException e) {
                            bank.setMaxAccountNoLength(null);
                        }
                    }

                    // Column 9: Verification (Y/N)
                    bank.setVerification(getBoolean(formatter, row, 9));

                    // Column 10: Active Status (S or other)
                    bank.setIsActive(getStatus(formatter, row, 10));

                    // Column 11: Blocked (true/false)
                    bank.setIsBlocked(getBoolean(formatter, row, 11));

                    bank.setCreatedBy(createdBy == null ? "SYSTEM" : createdBy);
                    bank.setCreatedOn(LocalDateTime.now());

                    batch.add(bank);

                    if (batch.size() >= BULK_BATCH_SIZE) {
                        inserted += flushBankBatch(batch);
                        batch = new ArrayList<>(BULK_BATCH_SIZE);
                    }

                } catch (Exception ex) {
                    errorCount++;
                    String error = "Row " + (rowIndex + 1) + " -> " + ex.getMessage();
                    log.error(error, ex);

                    if (errors.size() < 50) {
                        errors.add(error);
                    }
                }
            }

            if (!batch.isEmpty()) {
                inserted += flushBankBatch(batch);
            }

        } catch (Exception ex) {
            log.error("Upload failed", ex);
            throw new RuntimeException("Upload failed", ex);
        }

        return BankBulkUploadResult
                .builder()
                .totalRows(totalRows)
                .inserted(inserted)
                .skipped(0)
                .errorCount(errorCount)
                .elapsedMs(System.currentTimeMillis() - startMs)
                .errors(errors)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int flushBankBatch(List<NetworkBankMaster> batch) {
        bankMasterRepository.saveAll(batch);
        bankMasterRepository.flush();
        return batch.size();
    }

    /* HELPERS */

    private String getString(
            DataFormatter formatter,
            Row row,
            int col
    ) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return formatter.formatCellValue(cell);
    }

    private Integer getInteger(
            DataFormatter formatter,
            Row row,
            int col
    ) {
        String value = getString(formatter, row, col);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getBoolean(
            DataFormatter formatter,
            Row row,
            int col
    ) {
        String value = getString(formatter, row, col);
        if (value == null || value.isBlank()) {
            return false;
        }
        value = value.trim();
        return value.equalsIgnoreCase("Y")
                || value.equalsIgnoreCase("TRUE")
                || value.equals("1");
    }

    private Boolean getStatus(
            DataFormatter formatter,
            Row row,
            int col
    ) {
        String value = getString(formatter, row, col);
        return "S".equalsIgnoreCase(value);
    }

}
