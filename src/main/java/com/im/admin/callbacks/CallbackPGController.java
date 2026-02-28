package com.im.admin.callbacks;

import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.UnisusPGService;
import com.im.admin.service.impl.PaymentGatewayServiceImpl;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pg/callback")
@RequiredArgsConstructor
@Slf4j
public class CallbackPGController {

    private final RunpaisaPGServiceImpl paymentGatewayService;
    private final UnisusPGService unisusPGService;


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

    // UNISUS CALLBACKwa
    @PostMapping("/unisus")
    public ResponseEntity<String> unisusCallback(
            @RequestBody UnisusWebhookRequest request) {

        log.info("Unisus callback received");

        UnisusWebhookResponse response =
                unisusPGService.processWebhook(request);

        return ResponseEntity.ok(response.getStatusMsg());
    }
}
