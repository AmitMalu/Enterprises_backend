package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.CashfreeCreateOrderRequest;
import com.scs.admin.request.ScsPgCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import com.scs.admin.service.ScsPGService;
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
public class ScsInitiator implements PaymentGatewayInitiator {

    @Autowired
    private ScsPGService scsPGService;


    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws JsonProcessingException {

        log.info("Initiating SCS Payment Link | initiatedBy={}, subProductId={}, amount={}, lat={}, long={}",
                command.getInitiatedBy(),
                command.getSubProductId(),
                command.getAmount(),
                requestHeaderMetaDtoMeta.getLatitude(),
                requestHeaderMetaDtoMeta.getLongitude());

        // ===== 1. Prepare Request =====
        ScsPgCreateOrderRequest scsPgCreateOrderRequest = new ScsPgCreateOrderRequest();

        scsPgCreateOrderRequest.setGeoLocation(requestHeaderMetaDtoMeta.getLatitude() +","+ requestHeaderMetaDtoMeta.getLongitude());
        scsPgCreateOrderRequest.setClientIp(requestHeaderMetaDtoMeta.getIpAddress());
        scsPgCreateOrderRequest.setMedium(requestHeaderMetaDtoMeta.getMedium());
        scsPgCreateOrderRequest.setHolderMobileNumber(command.getCardHolderMobile());
        scsPgCreateOrderRequest.setCardHolderName(command.getCardHolderName());
        scsPgCreateOrderRequest.setAmount(String.valueOf(command.getAmount()));
        scsPgCreateOrderRequest.setPaymentMode("credit_card");
        scsPgCreateOrderRequest.setInitiatedBy(command.getInitiatedBy());
        scsPgCreateOrderRequest.setSubProductId(command.getSubProductId());
        scsPgCreateOrderRequest.setScsSubProductName(command.getScsSubProductName());

        log.debug("Customer details mapped | name={}, email={}, phone={}",
                command.getCardHolderName(),
                command.getEmailId(),
                command.getCardHolderMobile());

        // ===== 3. Create Payment Link =====
        log.info("Calling SCS Payment Link API...");

        PgUnifiedOrderResponse apiResponse = scsPGService.createOrder(scsPgCreateOrderRequest, requestHeaderMetaDtoMeta);

        log.info("Payment Link generated successfully | paymentUrl={}", apiResponse.getPaymentLink());

        // ===== 4. Build Response =====
        PgUnifiedOrderResponse response = PgUnifiedOrderResponse.builder()
                .vendor("SCS PG")
                .orderId(null) // optional: fetch from DB if needed
                .paymentLink(apiResponse.getPaymentLink())
                .orderToken(null)
                .status("INITIATED")
                .build();

        log.info("SCS Payment Link flow completed | status=INITIATED");

        return response;
    }

    @Override
    public String getVendorName() {
        return "SCS Payment Gateway";
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
