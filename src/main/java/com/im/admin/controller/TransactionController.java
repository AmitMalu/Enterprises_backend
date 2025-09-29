package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.request.UpdateSettlementStatusRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TransactionService;
import com.im.admin.util.CommonMethods;
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
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

//    @PostMapping("/fetch-card-details")
//    public ResponseEntity<String> getCardOperatorDetails(@Valid @RequestBody CardTypeRequestDto cardTypeRequestDto) {
//        log.info("Received request to fetch card details for BIN: {}", cardTypeRequestDto.getCardBinNo());
//        String result = transactionService.getOperatorDetailsByCardBIN(cardTypeRequestDto);
//        return ResponseEntity.ok(result);
//    }

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
