package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.CashfreePGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NftCashfreeInitiator implements PaymentGatewayInitiator {

    @Autowired
    private CashfreePGService cashfreePGService;

    @Override
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command,
                                           Double latitude,
                                           Double longitude) throws JsonProcessingException {

        log.info("Initiating Cashfree Payment Link | initiatedBy={}, subProductId={}, amount={}, lat={}, long={}",
                command.getInitiatedBy(),
                command.getSubProductId(),
                command.getAmount(),
                latitude,
                longitude);

        // ===== 1. Prepare Request =====
        CashfreeCreateOrderRequest createOrderRequest = new CashfreeCreateOrderRequest();

        createOrderRequest.setOrder_amount(command.getAmount());
        createOrderRequest.setInitiatedBy(command.getInitiatedBy());
        createOrderRequest.setSubProductId(command.getSubProductId());

        log.debug("Mapped basic request | initiatedBy={}, subProductId={}, amount={}",
                createOrderRequest.getInitiatedBy(),
                createOrderRequest.getSubProductId(),
                createOrderRequest.getOrder_amount());

        // ===== 2. Customer Details =====
        CashfreeCreateOrderRequest.CustomerDetails customerDetails =
                new CashfreeCreateOrderRequest.CustomerDetails();

        customerDetails.setCustomer_name(command.getCardHolderName());
        customerDetails.setCustomer_email(command.getEmailId());
        customerDetails.setCustomer_phone(command.getCardHolderMobile());

        createOrderRequest.setCustomer_details(customerDetails);

        log.debug("Customer details mapped | name={}, email={}, phone={}",
                command.getCardHolderName(),
                command.getEmailId(),
                command.getCardHolderMobile());

        // ===== 3. Create Payment Link =====
        log.info("Calling Cashfree Payment Link API...");

        PgUnifiedOrderResponse apiResponse = cashfreePGService.createOrder(createOrderRequest, latitude, longitude);

        String paymentUrl = apiResponse.getPaymentLink();

        log.info("Payment Link generated successfully | paymentUrl={}", paymentUrl);

        // ===== 4. Build Response =====
        PgUnifiedOrderResponse response = PgUnifiedOrderResponse.builder()
                .vendor("Cashfree PG")
                .orderId(null) // optional: fetch from DB if needed
                .paymentLink(paymentUrl)
                .orderToken(null)
                .status("INITIATED")
                .build();

        log.info("Cashfree Payment Link flow completed | status=INITIATED");

        return response;
    }

    @Override
    public String getVendorName() {
        return "NFT Cashfree Payment Gateway";
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {

       return cashfreePGService.checkStatus(orderId);

    }

    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        return null;
    }
}
