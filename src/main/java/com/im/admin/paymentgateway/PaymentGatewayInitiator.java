package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentGatewayInitiator {

    PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws JsonProcessingException;

    String getVendorName();

    // CHECK STATUS
    PgStatusResponse checkStatus(String orderId);

    // VALIDATE
    CommonResponse<String> validate(PgTransactionRequestDto request);
}
