package com.im.admin.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.commons.LoggableApi;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.orchestrator.PGOrchestrator;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@LoggableApi
@RestController
@RequestMapping("/api/pg")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentGatewayController {

    private final PGOrchestrator pgOrchestrator;
    private final RunpaisaPGServiceImpl paymentGatewayService;


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
            @RequestBody PGCreateOrderCommand request,
            @RequestHeader("Latitude") Double latitude,
            @RequestHeader("Longitude") Double longitude,
            @RequestHeader("ipaddress") String ipAddress) throws JsonProcessingException {

        long startTime = System.currentTimeMillis();
        log.info("======= START: createOrder =======");
        log.info("Request: {}", request);
        log.info("Location: Latitude={}, Longitude={}", latitude, longitude);
        log.info("IP Address: {}", ipAddress);

        // Validate IP address format
        if (!isValidIpAddress(ipAddress)) {
            log.error("Invalid IP address format: {}", ipAddress);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(400, "Invalid IP address format. Expected format: 1.22.36.186"));
        }

        // Validate location
        if (latitude == null || longitude == null) {
            log.error("Missing location headers");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(400, "Latitude and Longitude are required"));
        }

        try {
            PgUnifiedOrderResponse result = pgOrchestrator.initiate(
                    request, latitude, longitude, ipAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Order created successfully in {}ms: {}", duration, result);
            log.info("======= END: createOrder - SUCCESS =======");

            return ResponseEntity.ok(
                    CommonResponse.success(result, "Order created successfully")
            );

        } catch (IllegalArgumentException e) {
            log.error("Validation error: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(400, e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error creating order: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to create order. Please try again."));
        }
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        // IPv4 pattern (e.g., 1.22.36.186)
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.trim().matches(ipv4Pattern);
    }

//    @PostMapping("/order-status")
//    @Transactional
//    public ResponseEntity<?> orderStatus(@RequestParam("orderId") String orderId) {
//        log.info("Received request for order status, orderId: {}", orderId);
//        ResponseEntity<?> response = pgOrchestrator.checkStatus(orderId);
//        log.info("Order status response: {}", response);
//        return response;
//    }

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
