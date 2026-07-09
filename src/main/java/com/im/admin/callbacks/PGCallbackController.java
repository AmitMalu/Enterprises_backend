package com.im.admin.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.*;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import com.im.admin.util.IdempotencyServiceUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
    private final ImPGService imPGService;


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
    public ResponseEntity<String> handleRazorpayCallback(
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_payment_link_id") String plinkId,
            @RequestParam("razorpay_payment_link_reference_id") String orderId,
            @RequestParam("razorpay_payment_link_status") String status,
            @RequestParam("razorpay_signature") String signature) {

        log.info("Callback received: paymentId={}, plinkId={}, orderId={}, status={}",
                paymentId, plinkId, orderId, status);

        try {

            razorPayPGService.processCallback(paymentId, plinkId, orderId, status, signature);

            return ResponseEntity.ok("Payment processed");

        } catch (Exception e) {
            log.error("Callback processing failed", e);
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
        log.info("Idempotency Key >>> {}", idempotencyKey);

        // LOCK CHECK
        boolean acquired = idempotencyService.tryAcquire(idempotencyKey);

        if (!acquired) {
            log.warn("Duplicate webhook skipped: {}", idempotencyKey);
            return ResponseEntity.ok("Duplicate ignored");
        }
//        boolean isValid = verifySignature(rawPayload, signature, timestamp);
//
//        if (!isValid) {
//            log.error("SIGNATURE FAILED");
//            return ResponseEntity.status(400).body("Invalid signature");
//        }

        log.info("SIGNATURE VERIFIED");

        cashfreePGService.processWebhook(rawPayload, idempotencyKey);

        return ResponseEntity.ok("OK");
    }

    // RUNPAISA CALLBACK
    @PostMapping("/im")
    public void imCallback(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) throws Exception {

        log.info("IM callback received");

        imPGService.handleCallback(payload, request);

    }

}