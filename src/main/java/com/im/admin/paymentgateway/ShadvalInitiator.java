package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.ShadvalPayCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.ShadvalPGService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author SHUBHAM KHOPADE
 */
public class ShadvalInitiator implements PaymentGatewayInitiator {

    @Autowired
    private ShadvalPGService shadvalPGService;


    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDto) throws JsonProcessingException {

        ShadvalPayCreateOrderRequest request = new ShadvalPayCreateOrderRequest();

        request.setCustomerName(command.getCardHolderName());
        request.setEmailId(command.getEmailId());
        request.setAmount(command.getAmount());
        request.setMobileNo(command.getCardHolderMobile());
        request.setRemark(command.getNarration());

        String paymentUrl = shadvalPGService.createOrder(request, requestHeaderMetaDto);

        return PgUnifiedOrderResponse.builder()
                .vendor("SHADVAL")
                .orderId(null)
                .paymentLink(paymentUrl)
                .orderToken(null)
                .status("INITIATED")
                .build();

    }

    @Override
    public String getVendorName() {
        return "ShadvalPay Payment Gateway";
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
