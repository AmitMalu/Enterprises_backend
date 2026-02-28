package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentInitiator {

    PgUnifiedOrderResponse initiate(PGCreateOrderCommand command) throws JsonProcessingException;

    String getVendorName();

    // STATUS
    ResponseEntity<?> checkStatus(String orderId);

    // CALLBACK
    //String handleCallback(Map<String, String> payload,
     //                     HttpServletRequest request) throws JsonProcessingException;

    // VALIDATE
    CommonResponse<String> validate(PgTransactionRequestDto request);
}
