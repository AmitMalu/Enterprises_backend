package com.scs.admin.controller;

import com.scs.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("api/fingpay")
public class AepsMerchantEncryptionController {

    @Autowired
    ResourceLoader resourceLoader;


    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody String rawJson) throws Exception {

        Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
        String certificate = new String(resource.getInputStream().readAllBytes());

        // Encrypt JSON using util
        FingpayEncryptionUtil.FingpayRequestPayload payload =
                FingpayEncryptionUtil.buildFinalRequestFromRawJson(rawJson, certificate);

        return Map.of(
                "trnTimestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                "hash", payload.hash,
                "eskey", payload.eskey,
                "body", payload.body
        );
    }
}
