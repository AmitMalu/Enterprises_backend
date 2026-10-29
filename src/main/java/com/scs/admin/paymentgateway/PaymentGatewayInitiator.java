package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
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
