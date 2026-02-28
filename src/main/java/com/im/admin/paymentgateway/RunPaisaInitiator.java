package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunPaisaInitiator implements PaymentInitiator {

    private final RunpaisaPGServiceImpl runPaisaServiceImpl;

    @Override
    public String getVendorName() {
        return "Runpaisa Payment Gateway";
    }

    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command) throws JsonProcessingException {

        CreateOrderRequest req = new CreateOrderRequest();
        req.setInitiatedBy(command.getInitiatedBy());
        req.setProductId(command.getProductId());
        req.setSubProductId(command.getSubProductId());
        req.setAmount(command.getAmount());
        req.setMerchantRef(command.getMerchantRef());
        req.setCardBinNo(command.getCardBinNo());
        req.setUsername(command.getUsername());
        req.setCallbackUrl(command.getCallbackUrl());
        req.setTransactionScheme(command.getSchemeDetails());

        ResponseEntity<CommonResponse<String>> response =
                runPaisaServiceImpl.createOrder(req);

        String rawJson = Objects.requireNonNull(response.getBody()).getData();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(rawJson);

        return PgUnifiedOrderResponse.builder()
                .vendor("RUNPAISA")
                .orderId(json.path("order_id").asText())
                .paymentLink(json.path("paymentLink").asText())
                .orderToken(json.path("order_token").asText())
                .status(json.path("order_staus").asText())
                .build();    }

    @Override
    public ResponseEntity<?> checkStatus(String orderId) {
        return runPaisaServiceImpl.orderStatus(orderId);
    }


    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return runPaisaServiceImpl.validateTransaction(request);
    }
}
