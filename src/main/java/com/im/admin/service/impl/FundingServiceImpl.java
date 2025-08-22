package com.im.admin.service.impl;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.dto.RetailerTransactionResponseDto;
import com.im.admin.dto.SettlementPendingRequestDto;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.RetailerPostpaidTransactionDetailsRepository;
import com.im.admin.repositories.RetailerPrepaidTransactionDetailsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.response.BulkFundResponse;
import com.im.admin.service.FundingService;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Slf4j
public class FundingServiceImpl  implements FundingService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRepository vendorRepository;


    @Transactional
    public String fundsCreditDebit(FundCreditDebitDto dto) {
        // Generate base transaction ID
        String baseTxnId = commonMethods.generateBaseTransactionId();

        // Extract values
        String userId = dto.getUserAccountId();
        WalletType walletType = WalletType.fromString(dto.getWalletType());
        TransactionType transactionType = TransactionType.fromString(dto.getFundingType());

        BigDecimal amount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal confirmAmount = BigDecimal.valueOf(dto.getConfirmAmount());

        // Validate amount match
        if (amount.compareTo(confirmAmount) != 0) {
            throw new IllegalArgumentException("Amount and Confirm Amount do not match");
        }

        // Resolve wallet
        AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), userId);

        // Generate transaction ID prefix based on type
        String txnIdPrefix = transactionType == TransactionType.CREDIT ? "FCR" : "FDR";
        String txnId = txnIdPrefix + baseTxnId;

        commonMethods.performWalletUpdate(
                walletAmountTable,
                userId,
                walletType,
                amount,
                transactionType,
                txnId,
                transactionType.name(),
                dto.getRemarks()
        );

        return String.format("Transaction %s successful", transactionType.name());
    }

    @Override
    public void downloadSampleFile(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bulk-credit-debit-sample.xlsx");

        String[] headers = {
                "fundingType", "userAccountId", "walletType",
                "amount", "confirmAmount", "narrationType", "remarks"
        };

        Object[] sampleValues = {
                "CREDIT or DEBIT", "ABC-RT-001", "PREPAID or POSTPAID",
                1000.00, 1000.00, "FUND_ADDITION", "Bulk upload example"
        };

        try (Workbook workbook = new XSSFWorkbook();
             ServletOutputStream out = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("Credit-Debit");

            createRow(sheet, 0, headers);

            createRow(sheet, 1, sampleValues);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

    /**
     * Helper method to create a row with values
     */
    private void createRow(Sheet sheet, int rowIndex, Object[] values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number) {
                row.createCell(i).setCellValue(((Number) values[i]).doubleValue());
            } else {
                row.createCell(i).setCellValue(String.valueOf(values[i]));
            }
        }
    }

    @Override
    @Transactional
    public List<BulkFundResponse> processBulkFile(MultipartFile file) {
        List<BulkFundResponse> responses = new ArrayList<>();

        // create a thread pool based on CPU cores
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<Future<BulkFundResponse>> futures = new ArrayList<>();

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                FundCreditDebitDto dto = new FundCreditDebitDto();
                dto.setUserAccountId(row.getCell(0).getStringCellValue());
                dto.setWalletType(row.getCell(1).getStringCellValue());
                dto.setFundingType(row.getCell(2).getStringCellValue());
                dto.setAmount(row.getCell(3).getNumericCellValue());
                dto.setConfirmAmount(row.getCell(4).getNumericCellValue());
                dto.setRemarks(row.getCell(5).getStringCellValue());

                int rowNum = i + 1; // keep track of Excel row for error message

                // Submit async task
                futures.add(executorService.submit(() -> processSingleRow(dto, rowNum)));
            }

            // Collect results
            for (Future<BulkFundResponse> future : futures) {
                try {
                    responses.add(future.get()); // blocks until task completes
                } catch (Exception e) {
                    BulkFundResponse failed = new BulkFundResponse();
                    failed.setStatus("FAILED");
                    failed.setMessage("Unexpected error: " + e.getMessage());
                    responses.add(failed);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }

        return responses;
    }

    /**
     * Process a single Excel row into a transaction safely.
     */
    private BulkFundResponse processSingleRow(FundCreditDebitDto dto, int rowNum) {
        BulkFundResponse response = new BulkFundResponse();
        response.setUserAccountId(dto.getUserAccountId());
        response.setWalletType(dto.getWalletType());
        response.setFundingType(dto.getFundingType());
        response.setAmount(BigDecimal.valueOf(dto.getAmount()));

        try {
            String result = fundsCreditDebit(dto);
            response.setStatus("SUCCESS");
            response.setMessage(result);
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setMessage("Row " + rowNum + ": " + e.getMessage());
        }

        return response;
    }

    @Override
    public Page<RetailerTransactionResponseDto> getSettlementPendingTransactions(SettlementPendingRequestDto request, int page, int size) {

        log.info("Fetching SETTLEMENT PENDING transactions for walletType: {}", request.getWalletType());

        // fetch subProductName from DB
        String subProductName = subProductsRepository.findById(request.getSubProductId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid subProductId"))
                .getSubProductTitle();

        // fetch vendorName if vendorId present
        String vendorName = null;
        if (request.getVendorId() != null && !request.getVendorId().isBlank()) {
            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vendorId"))
                    .getVendorName();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedDateOfTransaction").descending());

        Page<RetailerTransactionResponseDto> responsePage;

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            responsePage = retailerPrepaidTransactionDetailsRepository.findFilteredTransactions(
                    request.getFromDate(),
                    request.getToDate(),
                    subProductName,
                    vendorName,
                    pageable
            ).map(txn -> new RetailerTransactionResponseDto(
                    txn.getId(),
                    txn.getUpdatedDateOfTransaction(),
                    txn.getRetailerId(),
                    txn.getMobileNo(),
                    txn.getSchemeName(),
                    txn.getAmount(),
                    txn.getTranStatus(),
                    txn.getTransactionType(),
                    txn.getRemarks(),
                    txn.getTransactionId(),
                    txn.getMedium(),
                    txn.getSubProductName()
            ));

        } else if ("POSTPAID".equalsIgnoreCase(request.getWalletType())) {
            responsePage = retailerPostpaidTransactionDetailsRepository.findFilteredTransactions(
                    request.getFromDate(),
                    request.getToDate(),
                    subProductName,
                    vendorName,
                    pageable
            ).map(txn -> new RetailerTransactionResponseDto(
                    txn.getId(),
                    txn.getUpdatedDateOfTransaction(),
                    txn.getRetailerId(),
                    txn.getMobileNo(),
                    txn.getSchemeName(),
                    txn.getAmount(),
                    txn.getTranStatus(),
                    txn.getTransactionType(),
                    txn.getRemarks(),
                    txn.getTransactionId(),
                    txn.getMedium(),
                    txn.getSubProductName()
            ));
        } else {
            throw new IllegalArgumentException("Invalid walletType. Allowed values: PREPAID / POSTPAID");
        }

        return responsePage;
    }


}
