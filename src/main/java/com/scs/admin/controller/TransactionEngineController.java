package com.scs.admin.controller;


import com.scs.admin.dto.*;
import com.scs.admin.request.UpdateSettlementStatusRequest;
import com.scs.admin.response.BulkTransactionStatusResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.TransactionEngineService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
@Slf4j
public class TransactionEngineController {

    @Autowired
    private TransactionEngineService transactionService;

    @PostMapping("/fetch-card-bin-details")
    public ResponseEntity<CommonResponse<CardBinDetailsResponseDto>> getCardBinDetails(
            @Valid @RequestBody CardTypeRequestDto cardTypeRequestDto) {

        log.info("Received Request To Fetch Card Bin Details for BIN: {}", cardTypeRequestDto.getCardBinNo());
        CommonResponse<CardBinDetailsResponseDto> response = transactionService.getDetailsByCardBINNumber(cardTypeRequestDto);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }


    @PostMapping("/fetch-scheme")
    public ResponseEntity<?> getSchemeDetails(@RequestBody TransactionSchemeDetailsRequestDto request) {
        log.info("Received request to fetch scheme details for transaction: {}", request);
        TransactionSchemeDetailsResponseDto schemeDetails = transactionService.getTransactionSchemeDetails(request);
        return ResponseEntity.ok(schemeDetails);
    }

    @PostMapping("/process-transaction")
    public ResponseEntity<String> processTransaction(@RequestBody TransactionSchemeDetailsRequestDto request, @RequestParam String vendorCallbackStatus) {
        log.info("Received request to process transaction: {}", request);
        String response = transactionService.processTransaction(request, vendorCallbackStatus);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-status/bulk")
    public ResponseEntity<String> updateTransactionStatusBulk(@RequestBody UpdateTransactionStatusRequest request) {
        transactionService.updateTransactionStatus(request);
        return ResponseEntity.ok("Bulk transaction status update completed successfully");
    }

    @PostMapping("/update-pending-status/bulk")
    public ResponseEntity<CommonResponse<BulkTransactionStatusResponse>>
    updatePendingTransactionStatusBulk(@RequestBody UpdateTransactionStatusRequest request) {

        CommonResponse<BulkTransactionStatusResponse> response =
                transactionService.updatePendingTransactionStatus(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/isSettlement/filter-by")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> getIsSettlementTransactions(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam String subProductName,
            @RequestParam String settlementType,
            @RequestParam(required = false) String walletType) {

        List<Map<String, Object>> finalResults = new ArrayList<>();
        List<String> walletTypesToSearch = (walletType == null || walletType.isEmpty())
                ? Arrays.asList("PREPAID", "POSTPAID")
                : List.of(walletType.toUpperCase());

        for (String wt : walletTypesToSearch) {
            finalResults.addAll(transactionService.getSettlementTransactionsByFilter(
                    fromDate, toDate, subProductName, settlementType, wt));
        }

        String message = finalResults.isEmpty()
                ? "No transactions found for the given filter"
                : "Transactions fetched successfully";

        CommonResponse<List<Map<String, Object>>> response = new CommonResponse<>(
                200, message, finalResults
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/isSettlement/bulk")
    @Transactional
    public ResponseEntity<CommonResponse<String>> bulkUpdateSettlementStatus(
            @RequestBody UpdateSettlementStatusRequest request) {

        transactionService.bulkUpdateSettlementStatus(
                request.getTransactionIds(), request.getWalletType(), request.getNewStatus());

        return ResponseEntity.ok(new CommonResponse<>(200, "Success", "Bulk update completed"));
    }

}
