package com.scs.admin.service.impl;


import com.scs.admin.response.RunPaisaCallback;
import com.scs.admin.service.VendorCallbackParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CashfreeCallbackParser implements VendorCallbackParser {

    @Override
    public boolean supports(Map<String, String> payload) {
        boolean isSupported = "CASHFREE".equalsIgnoreCase(payload.get("PG_PARTNER"));
        log.info("Checking if payload is supported: PG_PARTNER={} | supported={}", payload.get("PG_PARTNER"), isSupported);
        return isSupported;
    }

    @Override
    public RunPaisaCallback parse(Map<String, String> payload) {
        log.info("Parsing RunPaisa payload: {}", payload);

        RunPaisaCallback callback = RunPaisaCallback.builder()
                .orderId(payload.get("orderId"))
                .status(payload.get("txStatus"))
                .bankTxnId(payload.get("bankTxnId"))
                .txnAmount(payload.get("orderAmount"))
                .errorDesc(payload.get("txMsg"))
                .txnMode(payload.get("paymentMode"))
                .build();

        log.info("Parsed RunPaisaCallback: {}", callback);
        return callback;
    }

}
