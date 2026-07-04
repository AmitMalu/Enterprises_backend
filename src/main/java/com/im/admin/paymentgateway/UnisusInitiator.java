package com.im.admin.paymentgateway;

import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.UnisusPayCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.UnisusPGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnisusInitiator implements PaymentGatewayInitiator {

    private final UnisusPGService unisusService;

    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDto) {

        UnisusPayCreateOrderRequest req =
                new UnisusPayCreateOrderRequest();

        req.setSubProductId(command.getSubProductId());
        req.setAmount(command.getAmount());
        req.setInitiatedBy(command.getInitiatedBy());

        String redirectUrl = unisusService.authenticate(req, requestHeaderMetaDto);

        // if needed fetch from DB by sessionId

        return PgUnifiedOrderResponse.builder()
                .vendor("UNISUS")
                .orderId(null) // if needed fetch from DB by sessionId
                .paymentLink(redirectUrl)
                .orderToken(null)
                .status("INITIATED")
                .build();
    }


    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return CommonResponse.success("ALLOWED", "Unisus validation");
    }

    @Override
    public String getVendorName() {
        return "UnisusPay Payment Gateway";
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {
        return null;
    }
}
