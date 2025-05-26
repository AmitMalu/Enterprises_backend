package com.im.admin.controller;


import com.im.admin.dto.FetchSubProductFromRetailerRequest;
import com.im.admin.entities.CardTypeMaster;
import com.im.admin.entities.SchemeAllocation;
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


    @PostMapping("/fetch/card-bin-no/{cardBinNo}")
    public ResponseEntity<?> fetchCardBinNo(@PathVariable String cardBinNo) {

        CardTypeMaster cardDetails = paymentGatewayService.fetchCardTypeMaster(cardBinNo);

        if (cardDetails != null) {
            return ResponseEntity.ok(new CommonResponse<>(200, "Card details fetched", cardDetails));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResponse<>(404, "Card details not found", null));
        }
    }
}
