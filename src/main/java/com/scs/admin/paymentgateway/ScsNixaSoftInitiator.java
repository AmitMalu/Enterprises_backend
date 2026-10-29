package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.NixaSoftCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import com.scs.admin.service.NixaSoftPGService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
public class ScsNixaSoftInitiator implements PaymentGatewayInitiator {

    private final NixaSoftPGService nixaSoftPGService;

    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDto) throws JsonProcessingException {

        NixaSoftCreateOrderRequest request = new NixaSoftCreateOrderRequest();

        request.setBeneficiaryName(command.getCardHolderName());
        request.setMobileNo(command.getCardHolderMobile());
        request.setEmailId(command.getEmailId());
        request.setAmount(String.valueOf(command.getAmount()));
        request.setRedirectUrl(command.getCallbackUrl());
        request.setRequestId(command.getCardBinNo());  //later we need to change this getValue
        request.setSubProductId(command.getSubProductId());
        request.setProductId(command.getProductId());
        request.setInitiatedBy(command.getInitiatedBy());


        String paymentLink = nixaSoftPGService.createOrder(request, requestHeaderMetaDto);

        return PgUnifiedOrderResponse.builder()
                .vendor("NixaSoft")
                .orderId(null) // if needed fetch from DB by sessionId
                .paymentLink(paymentLink)
                .orderToken(null)
                .status("INITIATED")
                .build();

    }

    @Override
    public String getVendorName() {
        return "SCS Nixasoft Payment Gateway";
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {

        nixaSoftPGService.checkStatus(orderId);
        return null;
    }

    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return null;
    }
}
