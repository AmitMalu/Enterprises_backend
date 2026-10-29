package com.scs.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.PgTransactionRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.Customer;
import com.scs.admin.request.RazorPayCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import com.scs.admin.service.RazorPayPGService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@RequiredArgsConstructor
public class RazorPayInitiator implements PaymentGatewayInitiator {

    @Autowired
    private RazorPayPGService razorPayPGService;


    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, RequestHeaderMetaDto requestHeaderMetaDto) throws JsonProcessingException {

        RazorPayCreateOrderRequest payCreateOrderRequest = new RazorPayCreateOrderRequest();

        payCreateOrderRequest.setAmount(command.getAmount().intValue()); // rupees
        payCreateOrderRequest.setProductId(command.getProductId());
        payCreateOrderRequest.setSubProductId(command.getSubProductId());
        payCreateOrderRequest.setInitiatedBy(command.getInitiatedBy());
        payCreateOrderRequest.setDescription(command.getNarration());

        Customer customer = new Customer();
        customer.setName(command.getCardHolderName());
        customer.setContact(command.getCardHolderMobile());
        customer.setEmail(command.getEmailId());

        payCreateOrderRequest.setCustomer(customer);

        String payment_url = razorPayPGService.createOrder(payCreateOrderRequest, requestHeaderMetaDto);

        return PgUnifiedOrderResponse.builder()
                .vendor("RazorPay")
                .orderId(null) // if needed fetch from DB by sessionId
                .paymentLink(payment_url)
                .orderToken(null)
                .status("INITIATED")
                .build();

    }

    @Override
    public String getVendorName() {
        return "Razorpay Payment Gateway";
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
