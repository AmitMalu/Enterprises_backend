package com.im.admin.callbacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

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
//
//    @PostMapping("/unisus")
//    public ResponseEntity<String> unisusCallback(
//            HttpServletRequest request) throws IOException {
//
//        log.info("Content-Type: {}", request.getContentType());
//
//        String body = request.getReader()
//                .lines()
//                .collect(Collectors.joining(System.lineSeparator()));
//
//        log.info("RAW BODY: {}", body);
//
//        return ResponseEntity.ok("OK");
//    }

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

}
