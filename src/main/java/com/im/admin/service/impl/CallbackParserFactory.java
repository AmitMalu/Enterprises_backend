package com.im.admin.service.impl;

import com.im.admin.service.VendorCallbackParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CallbackParserFactory {

    @Autowired
    private List<VendorCallbackParser> parsers;

    public VendorCallbackParser getParser(Map<String, String> payload) {
        log.info("Attempting to get parser for payload: {}", payload);

        VendorCallbackParser parser = parsers.stream()
                .filter(p -> p.supports(payload))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Unsupported vendor or missing PG_PARTNER field in payload: {}", payload);
                    return new RuntimeException("Unsupported vendor or missing PG_PARTNER field");
                });

        log.info("Parser found: {} for payload", parser.getClass().getSimpleName());
        return parser;
    }

}

