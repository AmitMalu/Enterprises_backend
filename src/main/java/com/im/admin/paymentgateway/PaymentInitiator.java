package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentInitiator {

    PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, Double latitude, Double longitude) throws JsonProcessingException;

    String getVendorName();

    // CHECK STATUS
    ResponseEntity<?> checkStatus(String orderId);

    // VALIDATE
    CommonResponse<String> validate(PgTransactionRequestDto request);
}
