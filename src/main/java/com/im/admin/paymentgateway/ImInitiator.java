package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.ImPgCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.ImPGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class ImInitiator implements PaymentGatewayInitiator {

    @Autowired
    private ImPGService imPGService;


    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws JsonProcessingException {

        log.info("Initiating IM Payment Link | initiatedBy={}, subProductId={}, amount={}, lat={}, long={}",
                command.getInitiatedBy(),
                command.getSubProductId(),
                command.getAmount(),
                requestHeaderMetaDtoMeta.getLatitude(),
                requestHeaderMetaDtoMeta.getLongitude());

        // ===== 1. Prepare Request =====
        ImPgCreateOrderRequest imPgCreateOrderRequest = new ImPgCreateOrderRequest();

        imPgCreateOrderRequest.setGeoLocation(requestHeaderMetaDtoMeta.getLatitude() +","+ requestHeaderMetaDtoMeta.getLongitude());
        imPgCreateOrderRequest.setClientIp(requestHeaderMetaDtoMeta.getIpAddress());
        imPgCreateOrderRequest.setMedium(requestHeaderMetaDtoMeta.getMedium());
        imPgCreateOrderRequest.setHolderMobileNumber(command.getCardHolderMobile());
        imPgCreateOrderRequest.setCardHolderName(command.getCardHolderName());
        imPgCreateOrderRequest.setAmount(String.valueOf(command.getAmount()));
        imPgCreateOrderRequest.setPaymentMode("credit_card");
        imPgCreateOrderRequest.setInitiatedBy(command.getInitiatedBy());
        imPgCreateOrderRequest.setSubProductId(command.getSubProductId());

        log.debug("Customer details mapped | name={}, email={}, phone={}",
                command.getCardHolderName(),
                command.getEmailId(),
                command.getCardHolderMobile());

        // ===== 3. Create Payment Link =====
        log.info("Calling IM Payment Link API...");

        PgUnifiedOrderResponse apiResponse = imPGService.createOrder(imPgCreateOrderRequest, requestHeaderMetaDtoMeta);

        log.info("Payment Link generated successfully | paymentUrl={}", apiResponse.getPaymentLink());

        // ===== 4. Build Response =====
        PgUnifiedOrderResponse response = PgUnifiedOrderResponse.builder()
                .vendor("IM PG")
                .orderId(null) // optional: fetch from DB if needed
                .paymentLink(apiResponse.getPaymentLink())
                .orderToken(null)
                .status("INITIATED")
                .build();

        log.info("IM Payment Link flow completed | status=INITIATED");

        return response;
    }

    @Override
    public String getVendorName() {
        return "IM Payment Gateway";
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {
        return null;
    }

    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return null;
    }
}
