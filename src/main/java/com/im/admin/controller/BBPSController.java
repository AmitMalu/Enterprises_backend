package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.response.BillRechargeCheckStatusResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BBPSService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bbps")
public class BBPSController {

    @Autowired
    private BBPSService bbpsService;

    @PostMapping("/provider-list")
    public ResponseEntity<CommonResponse<List<ProviderListResponseDTO.ProviderDTO>>> getProviderList(
            @RequestBody ProviderListRequestDTO requestDTO) {
        log.info("Received request for provider list: {}", requestDTO);
        ResponseEntity<CommonResponse<List<ProviderListResponseDTO.ProviderDTO>>> response =
                ResponseEntity.ok(bbpsService.getProviderList(requestDTO));
        log.info("Provider list response: {}", response);
        return response;
    }

    @PostMapping("/fetch-bill-details")
    public ResponseEntity<CommonResponse<FetchBillDetailsResponseDTO>> fetchBillDetails(
            @Valid @RequestBody FetchBillDetailsRequestDTO requestDTO) {
        log.info("Received request to fetch bill details: {}", requestDTO);
        try {
            FetchBillDetailsResponseDTO response = bbpsService.fetchBillDetails(requestDTO);
            log.info("Fetch bill details service response: {}", response);

            if (response.getStatus() == 1) {
                log.info("Fetch bill details successful: {}", response.getMessage());
                return ResponseEntity.ok(CommonResponse.success(response, response.getMessage()));
            } else {
                log.warn("Fetch bill details failed: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error(400, response.getMessage()));
            }

        } catch (Exception ex) {
            log.error("Exception in fetchBillDetails API: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500,"Something went wrong: " + ex.getMessage()));
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<CommonResponse<?>> recharge(
            @RequestBody BillRechargeRequestDTO request, @RequestHeader Double latitude, @RequestHeader Double longitude, @RequestHeader("ipaddress") String ipAddress) {
        log.info("Received recharge request: {}", request);
        CommonResponse<?> response = bbpsService.processRecharge(request, latitude, longitude, ipAddress);
        log.info("Recharge response: {}", response);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/receipt/{txnId}")
    public ResponseEntity<CommonResponse<FetchReceiptResponseDto>> fetchReceipt(
            @PathVariable String txnId) {
        log.info("Received receipt request: {}", txnId);
        CommonResponse<FetchReceiptResponseDto> response = bbpsService.fetchReceipt(txnId);
        log.info("Recharge response: {}", response);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

}
