package com.im.admin.controller;


import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.dto.RetailerTransactionResponseDto;
import com.im.admin.dto.RetailerTransactionResponseDtoCom;
import com.im.admin.dto.SettlementPendingRequestDto;
import com.im.admin.response.BulkFundResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.FundingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/funding")
public class FundingController {

    @Autowired
    private FundingService fundingService;

    @PostMapping("/credit-debit")
    public ResponseEntity<CommonResponse<String>> fundsCreditDebit(@RequestBody FundCreditDebitDto dto) {
        log.info("API Call: fundsCreditDebit with data: {}", dto);
        String result = fundingService.fundsCreditDebit(dto);
        log.info("Transaction processed successfully: {}", result);
        return ResponseEntity.ok(CommonResponse.success(result, "Transaction processed successfully"));
    }

    @GetMapping("/bulk-credit-debit/sample")
    public void downloadSampleFile(HttpServletResponse response) throws IOException {
        log.info("API Call: downloadSampleFile");
        fundingService.downloadSampleFile(response);
        log.info("Sample file downloaded successfully");
    }

    @PostMapping("/upload")
    public ResponseEntity<CommonResponse<List<BulkFundResponse>>> uploadBulkFile(
            @RequestParam("file") MultipartFile file, @RequestHeader Double latitude, @RequestHeader Double longitude) {

        log.info("API Call: uploadBulkFile");

        if (file.isEmpty()) {
            log.warn("Upload failed: file is empty");
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "File is required.", null)
            );
        }

        try {
            List<BulkFundResponse> responses = fundingService.processBulkFile(file, latitude, longitude);
            log.info("Bulk file processed successfully. Number of responses: {}", responses.size());
            return ResponseEntity.ok(
                    new CommonResponse<>(200, "Bulk file processed successfully", responses)
            );
        } catch (Exception e) {
            log.error("Error while processing bulk file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CommonResponse<>(500, "Error while processing file: " + e.getMessage(), null)
            );
        }
    }

    @PostMapping("/settlement")
    public ResponseEntity<CommonResponse> getSettlementPending(
            @RequestBody SettlementPendingRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API Call: getSettlementPending with request: {}", request);

        Page<RetailerTransactionResponseDto> transactions =
                fundingService.getSettlementPendingTransactions(request, page, size);

        log.info("success. Total records: {}", transactions.getTotalElements());

        return ResponseEntity.ok(
                new CommonResponse(200, "success", transactions)
        );
    }


    @PostMapping("/settlement-updated")
    public ResponseEntity<CommonResponse> getSettlementPendingUpdated(
            @RequestBody SettlementPendingRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API Call: getSettlementPending with request: {}", request);

        Page<RetailerTransactionResponseDto> transactions =
                fundingService.getSettlementPendingTransactionsUpdated(request, page, size);

        log.info("success. Total records: {}", transactions.getTotalElements());

        return ResponseEntity.ok(
                new CommonResponse(200, "success", transactions)
        );
    }

    @PostMapping("/transaction-report")
    public ResponseEntity<CommonResponse> getTransactionrepoort(
            @RequestBody SettlementPendingRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API Call: getTransactionReport with request: {}", request);

        Page<RetailerTransactionResponseDto> transactions =
                fundingService.getTransactionReport(request, page, size);

        log.info("success. Total records: {}", transactions.getTotalElements());

        return ResponseEntity.ok(
                new CommonResponse(200, "success", transactions)
        );
    }

    @PostMapping("/detailed-txn-report")
    public ResponseEntity<CommonResponse> getTransactionReportComAndChg(
            @RequestBody SettlementPendingRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API Call: getTransactionReport with request: {}", request);

        Page<RetailerTransactionResponseDtoCom> transactions =
                fundingService.getTransactionReportComWise(request, page, size);

        log.info("success. Total records: {}", transactions.getTotalElements());

        return ResponseEntity.ok(
                new CommonResponse(200, "success", transactions)
        );
    }

    @PostMapping("/get-networks/{hierarchyId}")
    public ResponseEntity<CommonResponse> getSettlementPending(
            @PathVariable String hierarchyId
           ) {

        List<String> retailersUnderHierarchy = fundingService.getRetailersUnderHierarchy(hierarchyId);

        return ResponseEntity.ok(
                new CommonResponse(200, "success", retailersUnderHierarchy)
        );
    }



}
