package com.im.admin.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.NixaSoftPGService;
import com.im.admin.service.SlpePGService;
import com.im.admin.service.UnisusPGService;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import com.razorpay.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    private final PgTransactionMappingRepository repository;


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
    public ResponseEntity<String> handleCallback(HttpServletRequest request) {

        log.info("Received Razorpay Callback: {}", request);

        try {

            String paymentId = request.getParameter("razorpay_payment_id");
            String plinkId = request.getParameter("razorpay_payment_link_id");
            String referenceId = request.getParameter("razorpay_payment_link_reference_id");
            String status = request.getParameter("razorpay_payment_link_status");
            String signature = request.getParameter("razorpay_signature");

            log.info("Callback received paymentId={}, plinkId={}, status={}",
                    paymentId, plinkId, status);

            // ===== VERIFY SIGNATURE =====
            JSONObject options = new JSONObject();
            options.put("razorpay_payment_id", paymentId);
            options.put("payment_link_id", plinkId);
            options.put("payment_link_reference_id", referenceId);
            options.put("payment_link_status", status);
            options.put("razorpay_signature", signature);

            String secret = "rzp_test_SUfGOOZaPcOkb7";

            boolean isValid = Utils.verifyPaymentLink(options, secret);

            if (!isValid) {
                log.error("Invalid callback signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid signature");
            }

            // VALID CALLBACK (BUT DON'T TRUST COMPLETELY)
            log.info("Callback signature verified for orderId={}", referenceId);

            //  DO NOT update DB here (important)
            // Just show response to user

            return ResponseEntity.ok("Payment Processing...");

        } catch (Exception e) {
            log.error("Error in callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error");
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {

            String webhookSecret = "YOUR_WEBHOOK_SECRET";

            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

            if (!isValid) {
                log.error("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            log.info("Webhook received event={}", event);

            if ("payment_link.paid".equals(event)) {

                JSONObject entity = json
                        .getJSONObject("payload")
                        .getJSONObject("payment_link")
                        .getJSONObject("entity");

                String plinkId = entity.getString("id");
                String status = entity.getString("status");

                String paymentId = entity
                        .getJSONArray("payments")
                        .getJSONObject(0)
                        .getString("payment_id");

                handleSuccess(plinkId, paymentId, status);

            } else if ("payment_link.expired".equals(event)) {

                JSONObject entity = json
                        .getJSONObject("payload")
                        .getJSONObject("payment_link")
                        .getJSONObject("entity");

                String plinkId = entity.getString("id");

                handleFailure(plinkId, "EXPIRED");

            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Webhook error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }


    private void handleSuccess(String plinkId, String paymentId, String status) {

        Optional<PgTransactionMapping> optTxn =
                repository.findByVendorSessionId(plinkId);


        if (optTxn.isEmpty()) {
            log.error("Transaction not found for plinkId={}", plinkId);
            return;
        }

        PgTransactionMapping txn = optTxn.get();

        // IDEMPOTENCY CHECK (VERY IMPORTANT)
        if ("SUCCESS".equalsIgnoreCase(txn.getStatus())) {
            log.info("Transaction already marked SUCCESS for orderId={}", txn.getOrderId());
            return;
        }

        txn.setStatus("SUCCESS");
        txn.setVendorSessionId(paymentId); // store Razorpay payment_id

        repository.save(txn);

        log.info("Payment SUCCESS updated for orderId={}, paymentId={}",
                txn.getOrderId(), paymentId);
    }

    private void handleFailure(String plinkId, String reason) {


        Optional<PgTransactionMapping> optTxn =
                repository.findByVendorSessionId(plinkId);


        if (optTxn.isEmpty()) {
            log.error("Transaction not found for plinkId={}", plinkId);
            return;
        }

        PgTransactionMapping txn = optTxn.get();

        if ("SUCCESS".equalsIgnoreCase(txn.getStatus())) {
            return; // already success, ignore
        }

        txn.setStatus("FAILED");

        repository.save(txn);

        log.info("Payment FAILED for orderId={}, reason={}",
                txn.getOrderId(), reason);
    }
}
