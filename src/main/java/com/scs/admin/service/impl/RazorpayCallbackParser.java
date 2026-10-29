package com.scs.admin.service.impl;

import com.scs.admin.response.RunPaisaCallback;
import com.scs.admin.service.VendorCallbackParser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RazorpayCallbackParser implements VendorCallbackParser {

    @Override
    public boolean supports(Map<String, String> payload) {
        return "RAZORPAY".equalsIgnoreCase(payload.get("PG_PARTNER"));
    }

    @Override
    public RunPaisaCallback parse(Map<String, String> payload) {
        return RunPaisaCallback.builder()
                .orderId(payload.get("ORDER_ID"))
                .status(payload.get("STATUS"))
                .bankTxnId(payload.get("BANK_TXNID"))
                .txnAmount(payload.get("TXN_AMOUNT"))
                .statusCode(payload.get("STATUS_CODE"))
                .errorDesc(payload.get("ERROR_DESC"))
                .build();
    }
}

