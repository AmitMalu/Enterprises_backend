package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BBPSService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bbps")
public class BBPSController {

    @Autowired
    private BBPSService bbpsService;

    @PostMapping("/provider-list")
    public ResponseEntity<CommonResponse<List<ProviderListResponseDTO.ProviderDTO>>> getProviderList(
            @RequestBody ProviderListRequestDTO requestDTO) {
        return ResponseEntity.ok(bbpsService.getProviderList(requestDTO));
    }

    @PostMapping("/fetch-bill-details")
    public ResponseEntity<CommonResponse<FetchBillDetailsResponseDTO>> fetchBillDetails(
            @Valid @RequestBody FetchBillDetailsRequestDTO requestDTO) {
        try {
            FetchBillDetailsResponseDTO response = bbpsService.fetchBillDetails(requestDTO);

            if (response.getStatus() == 1) {
                return ResponseEntity.ok(CommonResponse.success(response, response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error(400, response.getMessage()));
            }

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500,"Something went wrong: " + ex.getMessage()));
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<CommonResponse<BillRechargeResponseDTO>> recharge(
            @RequestBody BillRechargeRequestDTO request) {

        CommonResponse<BillRechargeResponseDTO> response = bbpsService.processRecharge(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

}
