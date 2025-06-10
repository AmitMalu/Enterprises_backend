package com.im.admin.controller;


import com.im.admin.dto.FetchSubProductFromRetailerRequest;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.entities.CardTypeMaster;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.BulkUpdateTransactionStatusRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Validated
public class PaymentGatewayController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;


    @PostMapping("/fetch/card-bin-no")
    public ResponseEntity<CommonResponse<RetailerCommissionResponseDTO>> fetchCardBinNo(
            @RequestBody PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO) {

        CommonResponse<RetailerCommissionResponseDTO> response =
                paymentGatewayService.fetchRetailerCommission(paymentGatewayCardBinRequestDTO);

        return ResponseEntity.ok(response);
    }

}
