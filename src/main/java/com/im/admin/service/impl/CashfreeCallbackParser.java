package com.im.admin.service.impl;


import com.im.admin.response.RunPaisaCallback;
import com.im.admin.service.VendorCallbackParser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CashfreeCallbackParser implements VendorCallbackParser {

    @Override
    public boolean supports(Map<String, String> payload) {
        return "CASHFREE".equalsIgnoreCase(payload.get("PG_PARTNER"));
    }

    @Override
    public RunPaisaCallback parse(Map<String, String> payload) {
        return RunPaisaCallback.builder()
                .orderId(payload.get("orderId"))
                .status(payload.get("txStatus"))
                .bankTxnId(payload.get("bankTxnId"))
                .txnAmount(payload.get("orderAmount"))
                .errorDesc(payload.get("txMsg"))
                .txnMode(payload.get("paymentMode"))
                .build();
    }
}
