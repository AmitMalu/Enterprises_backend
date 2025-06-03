package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TransactionService;
import com.im.admin.util.CommonMethods;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CommonMethods commonMethods;

    @PostMapping("/fetch-card-details")
    public ResponseEntity<String> getCardOperatorDetails(@Valid @RequestBody CardTypeRequestDto cardTypeRequestDto) {
        log.info("Received request to fetch card details for BIN: {}", cardTypeRequestDto.getCardBIN_No());
        String result = transactionService.getOperatorDetailsByCardBIN(cardTypeRequestDto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fetch-scheme")
    public ResponseEntity<?> getSchemeDetails(@RequestBody TransactionSchemeDetailsRequestDto request) {
        log.info("Received request to fetch scheme details for transaction: {}", request);
        TransactionSchemeDetailsResponseDto schemeDetails = transactionService.getTransactionSchemeDetails(request);
        return ResponseEntity.ok(schemeDetails);
    }

    @PostMapping("/process-transaction")
    public ResponseEntity<String> processTransaction(@RequestBody TransactionSchemeDetailsRequestDto request) {
        log.info("Received request to process transaction: {}", request);
        String response = transactionService.processTransaction(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-status")
    public ResponseEntity<String> updateTransactionStatus(@RequestBody UpdateTransactionStatusRequest request) {
        transactionService.updateTransactionStatus(request);
        return ResponseEntity.ok("Transaction status updated successfully");
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<TransactionSearchResponseDTO>>> getTransactions(
            @RequestParam String hierarchyId,
            @RequestParam(required = false) String walletType,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String status) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);

        List<TransactionSearchResponseDTO> finalResults = new ArrayList<>();

        // Decide which wallets to query
        List<String> walletTypesToSearch = new ArrayList<>();
        if (walletType == null || walletType.isEmpty()) {
            walletTypesToSearch.add("PREPAID");
            walletTypesToSearch.add("POSTPAID");
        } else {
            walletTypesToSearch.add(walletType.toUpperCase());
        }

        for (String wt : walletTypesToSearch) {
            finalResults.addAll(transactionService.getTransactions(
                    hierarchyId, transactionId, status, wt
            ));
        }

        CommonResponse<List<TransactionSearchResponseDTO>> response = new CommonResponse<>(
                200,
                "Transactions fetched successfully",
                finalResults
        );

        return ResponseEntity.ok(response);
    }

}
