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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;


@Service
@Slf4j
public class FundingServiceImpl implements FundingService {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Transactional
    public String fundsCreditDebit(FundCreditDebitDto dto) {
        log.info("Starting funds {} operation for userAccountId: {}, WalletType: {}, Amount: {}",
                dto.getFundingType(), dto.getUserAccountId(), dto.getWalletType(), dto.getAmount());

        // Generate base transaction ID
        String baseTxnId = commonMethods.generateBaseTransactionId();
        log.debug("Generated base transaction ID: {}", baseTxnId);

        // Extract values
        String userId = dto.getUserAccountId();
        WalletType walletType = WalletType.fromString(dto.getWalletType());
        TransactionType transactionType = TransactionType.fromString(dto.getFundingType());
        log.debug("Resolved WalletType: {}, TransactionType: {}", walletType, transactionType);

        BigDecimal amount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal confirmAmount = BigDecimal.valueOf(dto.getConfirmAmount());
        log.debug("Amount: {}, ConfirmAmount: {}", amount, confirmAmount);

        // Validate amount match
        if (amount.compareTo(confirmAmount) != 0) {
            log.error("Amount mismatch: Amount={} ConfirmAmount={}", amount, confirmAmount);
            throw new IllegalArgumentException("Amount and Confirm Amount do not match");
        }

        // Resolve wallet
        AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), userId);
        log.debug("Resolved walletAmountTable for user {}: {}", userId, walletAmountTable);

        // Generate transaction ID prefix based on type
        String txnIdPrefix = transactionType == TransactionType.CREDIT ? "FCR" : "FDR";
        String txnId = txnIdPrefix + baseTxnId;
        log.info("Generated transaction ID: {}", txnId);

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

        log.info("Transaction {} successful for user {} with txnId {}", transactionType.name(), userId, txnId);
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
    public List<BulkFundResponse> processBulkFile(MultipartFile file) {
        List<BulkFundResponse> responses = new ArrayList<>();

        // create a thread pool based on CPU cores
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<CompletableFuture<BulkFundResponse>> futures = new ArrayList<>();

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                FundCreditDebitDto dto = new FundCreditDebitDto();
                dto.setFundingType(row.getCell(0).getStringCellValue().trim());
                dto.setUserAccountId(row.getCell(1).getStringCellValue().trim());
                dto.setWalletType(row.getCell(2).getStringCellValue().trim());
                dto.setAmount(row.getCell(3).getNumericCellValue());
                dto.setConfirmAmount(row.getCell(4).getNumericCellValue());
                dto.setNarrationType(row.getCell(5).getStringCellValue());
                dto.setRemarks(row.getCell(6).getStringCellValue());

                int rowNum = i + 1; // keep track of Excel row for error message

                // Submit async task
                CompletableFuture<BulkFundResponse> future =
                        CompletableFuture.supplyAsync(() -> processSingleRow(dto, rowNum), executorService)
                                .exceptionally(ex -> {
                                    BulkFundResponse failed = new BulkFundResponse();
                                    failed.setStatus("FAILED");
                                    failed.setMessage("Row " + rowNum + ": " + ex.getMessage());
                                    return failed;
                                });

                futures.add(future);
            }

            responses = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

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
        response.setConfirmAmount(BigDecimal.valueOf(dto.getConfirmAmount()));
        response.setWalletType(dto.getWalletType());
        response.setFundingType(dto.getFundingType());
        response.setAmount(BigDecimal.valueOf(dto.getAmount()));

        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute(status -> {
                String result = fundsCreditDebit(dto); // now runs inside a TX
                response.setStatus("SUCCESS");
                response.setMessage(result);
                return null;
            });
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setMessage("Row " + rowNum + ": " + e.getMessage());
        }

        return response;
    }

    @Override
    public Page<RetailerTransactionResponseDto> getSettlementPendingTransactions(SettlementPendingRequestDto request, int page, int size) {

        log.info("Fetching transactions for walletType: {}", request.getWalletType());

        String subProductName = subProductsRepository.findById(request.getSubProductId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid subProductId"))
                .getSubProductTitle();

        String vendorName = null;
        if (request.getVendorId() != null && !request.getVendorId().isBlank()) {
            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vendorId"))
                    .getVendorName();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedDateOfTransaction").descending());

        List<String> statusList;
        if ("POST SETTLEMENT".equalsIgnoreCase(request.getTranStatus())) {
            statusList = Arrays.asList("SUCCESS", "FAILURE");
        } else if (request.getTranStatus() == null || request.getTranStatus().isBlank()) {
            statusList = null; // all records
        } else {
            statusList = Collections.singletonList(request.getTranStatus());
        }

        Page<RetailerTransactionResponseDto> responsePage;

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            // Only Prepaid
            responsePage = fetchPrepaid(request.getFromDate(), request.getToDate(),
                    subProductName, vendorName, statusList, pageable);

        } else if ("POSTPAID".equalsIgnoreCase(request.getWalletType())) {
            // Only Postpaid
            responsePage = fetchPostpaid(request.getFromDate(), request.getToDate(),
                    subProductName, vendorName, statusList, pageable);

        } else if (request.getWalletType() == null || request.getWalletType().isBlank()) {
            // Empty/null → Both Prepaid + Postpaid
            Page<RetailerTransactionResponseDto> prepaid = fetchPrepaid(request.getFromDate(), request.getToDate(),
                    subProductName, vendorName, statusList, pageable);

            Page<RetailerTransactionResponseDto> postpaid = fetchPostpaid(request.getFromDate(), request.getToDate(),
                    subProductName, vendorName, statusList, pageable);

            // Merge both pages safely
            List<RetailerTransactionResponseDto> merged = Stream.concat(prepaid.stream(), postpaid.stream())
                    .sorted(Comparator.comparing(
                            RetailerTransactionResponseDto::getUpdatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .collect(Collectors.toList());

            responsePage = new PageImpl<>(merged, pageable, merged.size());

        } else {
            // ❌ Invalid walletType
            throw new IllegalArgumentException("Invalid walletType: " + request.getWalletType());
        }

        return responsePage;
    }


    // Helper methods
    private Page<RetailerTransactionResponseDto> fetchPrepaid(LocalDate fromDate, LocalDate toDate,String subProductName, String vendorName, List<String> statusList, Pageable pageable) {
        return retailerPrepaidTransactionDetailsRepository.findFilteredTransactions(
                fromDate, toDate, subProductName, vendorName, statusList, pageable
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
    }

    private Page<RetailerTransactionResponseDto> fetchPostpaid(LocalDate fromDate, LocalDate toDate,String subProductName, String vendorName, List<String> statusList, Pageable pageable) {
        return retailerPostpaidTransactionDetailsRepository.findFilteredTransactions(
                fromDate, toDate, subProductName, vendorName, statusList, pageable
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
    }




}
