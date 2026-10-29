package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.ShadvalPayCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import com.scs.admin.service.ShadvalPGService;
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
