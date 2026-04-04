package com.im.admin.callbacks;

import com.cashfree.pg.Cashfree;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.*;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import com.im.admin.util.IdempotencyServiceUtil;
import com.razorpay.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private final RazorPayPGService razorPayPGService;
    private final CashfreePGService cashfreePGService;
    private final PgTransactionMappingRepository repository;
    private final IdempotencyServiceUtil idempotencyService;


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

    @GetMapping("/razorpay")
    public ResponseEntity<String> handleRazorPayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {

            razorPayPGService.processCallback(payload, signature);

            return ResponseEntity.ok("OK");

        } catch (IllegalArgumentException e) {
            log.error("Invalid webhook", e);
            return ResponseEntity.badRequest().body("Invalid signature");

        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    @PostMapping("/cashfree")
    public ResponseEntity<String> cashfreeCallback(
            @RequestBody String rawPayload,
            @RequestHeader("x-webhook-signature") String signature,
            @RequestHeader("x-webhook-timestamp") String timestamp,
            @RequestHeader(value = "x-idempotency-key", required = false) String idempotencyKey) {

        log.info("===== CASHFREE WEBHOOK START =====");

        log.info("RAW PAYLOAD >>> {}", rawPayload);
        log.info("Payload Length >>> {}", rawPayload.length());
        log.info("Signature >>> {}", signature);
        log.info("Timestamp >>> {}", timestamp);


        // LOCK CHECK
        boolean acquired = idempotencyService.tryAcquire(idempotencyKey);

        if (!acquired) {
            log.warn("Duplicate webhook skipped: {}", idempotencyKey);
            return ResponseEntity.ok("Duplicate ignored");
        }
        boolean isValid = verifySignature(rawPayload, signature, timestamp);

        if (!isValid) {
            log.error("SIGNATURE FAILED");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        log.info("SIGNATURE VERIFIED");

        cashfreePGService.processWebhook(rawPayload, idempotencyKey);

        return ResponseEntity.ok("OK");
    }

    public boolean verifySignature(String rawBody, String signature, String timestamp) {
        try {

            String secretKey = "cfsk_ma_test_c55beaca2b6adbe2630c91be6654e0ed_f9f1de22";

            // ===== DATA STRING =====
            String data = timestamp + rawBody;

            log.info("DATA STRING (timestamp + payload) >>> {}", data);
            log.info("DATA LENGTH >>> {}", data.length());

            // ===== HMAC =====
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = Base64.getEncoder().encodeToString(rawHmac);

            // ===== FINAL LOGS =====
            log.info("Generated Signature >>> {}", generatedSignature);
            log.info("Received Signature >>> {}", signature);

            boolean match = generatedSignature.equals(signature);

            log.info("Signature Match Result >>> {}", match ? "MATCH" : "MISMATCH");

            return match;

        } catch (Exception e) {
            log.error("Signature verification error", e);
            throw new RuntimeException(e);
        }
    }

}