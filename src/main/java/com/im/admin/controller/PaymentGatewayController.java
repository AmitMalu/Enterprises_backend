package com.im.admin.controller;


import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
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

    LocalDateTime currentDateTimeDate = LocalDateTime.now();

    @GetMapping("/generate-token")
    public ResponseEntity<String> generateToken() {
        log.info("******* generateToken at {} started *******", currentDateTimeDate);
        String token = paymentGatewayService.generateToken();
        log.info("******* generateToken at {} ends *******", currentDateTimeDate);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        return paymentGatewayService.createOrder(request);
    }

    @PostMapping("/order-status")
    public ResponseEntity<?> orderStatus(@RequestParam("orderId") String orderId) {
        return paymentGatewayService.orderStatus(orderId);
    }

    @PostMapping("/payment/callback")
    public ResponseEntity<String> handleCallback(@RequestBody Map<String, String> payload,
                                                 HttpServletRequest request) {
        log.info("Received callback request from IP: {}", request.getRemoteAddr());

        String result = paymentGatewayService.handleCallback(payload, request);

        log.info("Callback response sent: {}", result);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/fetch/card-bin-no")
    public ResponseEntity<CommonResponse<RetailerCommissionResponseDTO>> fetchCardBinNo(
            @RequestBody PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO) {

        CommonResponse<RetailerCommissionResponseDTO> response =
                paymentGatewayService.fetchRetailerCommission(paymentGatewayCardBinRequestDTO);

        return ResponseEntity.ok(response);
    }

}
