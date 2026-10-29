package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.SlpePgCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PayoutStatusResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import com.scs.admin.service.SlpePGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SlpeInitiator implements PaymentGatewayInitiator {

    private final SlpePGService slpePGService;

    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDto) throws JsonProcessingException {

        SlpePgCreateOrderRequest req = new SlpePgCreateOrderRequest();

        req.setSubProductId(command.getSubProductId());
        req.setAmount(command.getAmount());
        req.setInitiatedBy(command.getInitiatedBy());
        req.setCustomerName(command.getCardHolderName());
        req.setCustomerMobile(command.getCardHolderMobile());
        req.setCustomerEmail(command.getEmailId());

        String redirectUrl = slpePGService.createOrder(req, requestHeaderMetaDto);

        return PgUnifiedOrderResponse.builder()
                .vendor("SLPE PG")
                .orderId(null) // if needed fetch from DB by sessionId
                .paymentLink(redirectUrl)
                .orderToken(null)
                .status("INITIATED")
                .build();
    }

    @Override
    public String getVendorName() {
        return "SLPE Payment Gateway";
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {

        log.info("RUNPAISA checkStatus called. orderId={}", orderId);

        try {

            PgStatusResponse response = slpePGService.checkStatus(orderId);

            log.info("RUNPAISA checkStatus completed. orderId={}, httpStatus={}", orderId, response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Error during RUNPAISA checkStatus. orderId={}", orderId, e);
            throw e;
        }
    }

    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return null;
    }
}
