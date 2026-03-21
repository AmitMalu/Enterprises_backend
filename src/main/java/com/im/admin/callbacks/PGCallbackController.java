package com.im.admin.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.NixaSoftPGService;
import com.im.admin.service.SlpePGService;
import com.im.admin.service.UnisusPGService;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pg/callback")
@RequiredArgsConstructor
@Slf4j
public class PGCallbackController {

    private final RunpaisaPGServiceImpl paymentGatewayService;
    private final UnisusPGService unisusPGService;
    private final NixaSoftPGService nixaSoftPGService;
    private final SlpePGService slpePGService;


    // RUNPAISA CALLBACK
    @PostMapping("/runpaisa")
    public ResponseEntity<String> runpaisaCallback(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) throws Exception {

        log.info("RunPaisa callback received");

        String response =
                paymentGatewayService.handleCallback(payload, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/unisus")
    public ResponseEntity<UnisusWebhookResponse> unisusCallback(
            @RequestBody String rawBody) throws Exception {

        log.info("RAW CALLBACK: {}", rawBody);

        ObjectMapper mapper = new ObjectMapper();
        UnisusWebhookRequest request =
                mapper.readValue(rawBody, UnisusWebhookRequest.class);

        log.debug("Parsed request: {}", request);

        UnisusWebhookResponse response =
                unisusPGService.processWebhook(request);

        log.info("Sending response to Unisus: {}", response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/nixasoft")
    public ResponseEntity<String> handleCallback(
            @RequestBody NixaSoftCallbackRequest request) {

        log.info("Received NixaSoft Callback: {}", request);

        nixaSoftPGService.processCallback(request);

        return ResponseEntity.ok("Callback Processed");
    }

    @PostMapping("/slpe")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody Map<String, Object> payload) {

        log.info("Received SLPE webhook: {}", payload);

        slpePGService.processCallback(payload);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Webhook received");

        return ResponseEntity.ok(response);
    }
}
