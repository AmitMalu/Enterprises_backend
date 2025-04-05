package com.im.admin.controller;


import com.im.admin.dto.CardTypeRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsResponseDto;
import com.im.admin.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

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
}
