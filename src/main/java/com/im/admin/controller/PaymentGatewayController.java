package com.im.admin.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.orchestrator.PGOrchestrator;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.impl.PaymentGatewayServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pg")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentGatewayController {

    private final PGOrchestrator pgOrchestrator;
    private final PaymentGatewayServiceImpl paymentGatewayService;


    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<String>> validateTransaction(
            @RequestBody PgTransactionRequestDto request) {

        log.info("Received request to validate transaction: {}", request);
        CommonResponse<String> response = paymentGatewayService.validateTransaction(request);
        log.info("Transaction validation response: {}", response);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }

    @PostMapping("/create-order")
    public ResponseEntity<CommonResponse<PgUnifiedOrderResponse>> createOrder(
            @RequestBody PGCreateOrderCommand request) throws JsonProcessingException {
        log.info("Received request to create order: {}", request);
        PgUnifiedOrderResponse result =
                pgOrchestrator.initiate(request);
        log.info("Order creation response: {}", result);
        return ResponseEntity.ok(
                CommonResponse.success(result, "Order created successfully")
        );
    }

    @PostMapping("/order-status")
    @Transactional
    public ResponseEntity<?> orderStatus(@RequestParam("orderId") String orderId) {
        log.info("Received request for order status, orderId: {}", orderId);
        ResponseEntity<?> response = pgOrchestrator.checkStatus(orderId);
        log.info("Order status response: {}", response);
        return response;
    }

    @PostMapping("/fetch/card-bin-no")
    public ResponseEntity<CommonResponse<RetailerCommissionResponseDTO>> fetchCardBinNo(
            @RequestBody PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO) {

        log.info("Received request to fetch card bin no: {}", paymentGatewayCardBinRequestDTO);
        CommonResponse<RetailerCommissionResponseDTO> response =
                paymentGatewayService.fetchRetailerCommission(paymentGatewayCardBinRequestDTO);
        log.info("Card bin no fetch response: {}", response);
        return ResponseEntity.ok(response);
    }

}
