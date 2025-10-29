package com.im.admin.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PaymentGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/pg")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentGatewayController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<String>> validateTransaction(
            @RequestBody PgTransactionRequestDto request) {

        log.info("Received request to validate transaction: {}", request);
        CommonResponse<String> response = paymentGatewayService.validateTransaction(request);
        log.info("Transaction validation response: {}", response);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatusCode()));
    }

    @GetMapping("/generate-token")
    public ResponseEntity<String> generateToken() {
        log.info("******* generateToken started *******");
        String token = paymentGatewayService.generateToken();
        log.info("******* generateToken ends ******* token: {}", token);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @PostMapping("/create-order")
    @Transactional
    public ResponseEntity<CommonResponse<String>> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received request to create order: {}", request);
        ResponseEntity<CommonResponse<String>> response = paymentGatewayService.createOrder(request);
        log.info("Order creation response: {}", response.getBody());
        return response;
    }

    @PostMapping("/order-status")
    @Transactional
    public ResponseEntity<?> orderStatus(@RequestParam("orderId") String orderId) {
        log.info("Received request for order status, orderId: {}", orderId);
        ResponseEntity<?> response = paymentGatewayService.orderStatus(orderId);
        log.info("Order status response: {}", response);
        return response;
    }

    @PostMapping("/payment/callback")
    public ResponseEntity<String> handleCallback(@RequestBody Map<String, String> payload,
                                                 HttpServletRequest request) throws JsonProcessingException {
        log.info("Received callback request from IP: {}, payload: {}", request.getRemoteAddr(), payload);
        String result = paymentGatewayService.handleCallback(payload, request);
        log.info("Callback response sent: {}", result);
        return ResponseEntity.ok(result);
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
