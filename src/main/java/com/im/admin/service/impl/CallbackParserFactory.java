package com.im.admin.service.impl;

import com.im.admin.service.VendorCallbackParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CallbackParserFactory {

    @Autowired
    private List<VendorCallbackParser> parsers;

    public VendorCallbackParser getParser(Map<String, String> payload) {
        return parsers.stream()
                .filter(p -> p.supports(payload))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported vendor or missing PG_PARTNER field"));
    }
}

