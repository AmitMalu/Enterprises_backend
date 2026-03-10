package com.im.admin.service.impl;

import com.im.admin.callbacks.UnisusCallbackAsyncProcessor;
import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.UnisusAuthRequest;
import com.im.admin.request.UnisusPayCreateOrderRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusAuthResponse;
import com.im.admin.response.UnisusWebhookResponse;
import com.im.admin.service.UnisusPGService;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@Slf4j
@RequiredArgsConstructor
public class UnisusPGServiceImpl implements UnisusPGService {

    private final VendorRoutingRepository vendorRoutingRepository;
    private final VendorCredentialsRepository vendorCredentialsRepository;
    private final WebClient.Builder webClientBuilder;
    private final CommonMethods commonMethods;
    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final UnisusCallbackAsyncProcessor asyncProcessor;

    @Override
    public String authenticate(UnisusPayCreateOrderRequest req) {

        log.info("Starting Unisus authentication for subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getAmount());

        try {

            // ====== 1. Get Vendor & Credentials ======
            log.debug("Fetching VendorRouting for subProductId={}", req.getSubProductId());

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            log.debug("Resolved vendorId={}, productId={}",
                    vendor.getId(), vendor.getProduct().getId());

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("No credentials found"));

            log.info("Vendor credentials fetched successfully for vendorId={}", vendor.getId());

            // ====== 2. Build Authenticate Request ======
            UnisusAuthRequest authRequest = new UnisusAuthRequest(
                    credentials.getClientId(),
                    credentials.getUsername(),
                    credentials.getClientSecret()
            );

            log.info("Calling Unisus Authenticate API at URL={}", credentials.getTokenUrlUat());

            WebClient webClient = webClientBuilder.build();

            UnisusAuthResponse response = webClient.post()
                    .uri(credentials.getTokenUrlUat())
                    .bodyValue(authRequest)
                    .retrieve()
                    .bodyToMono(UnisusAuthResponse.class)
                    .block();

            log.debug("Received authentication response from Unisus: {}", response);

            if (response == null || response.getStatus() != 0) {
                log.error("Unisus authentication failed. Status={}, Message={}",
                        response != null ? response.getStatus() : null,
                        response != null ? response.getStatusMsg() : "null response");

                throw new RuntimeException(
                        "Authentication failed: " + response.getStatusMsg());
            }

            // ====== 3. Build Redirect URL ======
            String redirectUrl = credentials.getBaseUrlUat() +
                    "?SessionID=" + response.getSessionId() +
                    "&AccessKey=" + response.getAccessKey();

            log.info("Unisus Redirect URL created successfully for sessionId={}",
                    response.getSessionId());

            // STEP 1 – Create Internal Transaction FIRST
            String orderId = "ORD" + commonMethods.generateBaseTransactionId();

            PgTransactionMapping entity = new PgTransactionMapping();
            entity.setOrderId(orderId);
            entity.setAmount(req.getAmount());
            entity.setInitiatedBy(req.getInitiatedBy());
            entity.setSubProductId(req.getSubProductId());
            entity.setStatus("INITIATED");
            entity.setVendorSessionId(response.getSessionId());
            entity.setCardBinNo(req.getCardBinNo());
            entity.setVendorName("UnisusPay Payment Gateway");

            pgTransactionMappingRepository.save(entity);

            log.info("Transaction saved successfully. orderId={}, vendorSessionId={}, status=INITIATED",
                    orderId, response.getSessionId());

            return redirectUrl;

        } catch (Exception e) {
            log.error("Error during Unisus authentication for subProductId={}",
                    req.getSubProductId(), e);
            throw new RuntimeException("Unisus Authentication Failed");
        }
    }

    @Override
    public UnisusWebhookResponse processWebhook(
            UnisusWebhookRequest request) {

        log.info("Received Unisus Webhook. requestType={}, sessionId={}",
                request.getRequestType(), request.getSessionID());

        return switch (request.getRequestType()) {
            case 15 -> handlePgRequest(request);
            case 16 -> handlePgResponse(request);
            default -> {
                log.warn("Invalid Request Type received from Unisus: {}",
                        request.getRequestType());
                yield new UnisusWebhookResponse(
                        1,
                        "Invalid Request Type",
                        null
                );
            }
        };
    }

    private UnisusWebhookResponse handlePgRequest(
            UnisusWebhookRequest request) {

        log.info("Processing PG Request webhook. sessionId={}",
                request.getSessionID());

        PgTransactionMapping txn =
                pgTransactionMappingRepository
                        .findByVendorSessionId(String.valueOf(request.getSessionID()))
                        .orElseThrow(() -> {
                            log.error("Transaction not found for sessionId={}",
                                    request.getSessionID());
                            return new RuntimeException("Txn not found");
                        });

        log.info("Transaction found for sessionId={}, orderId={}",
                request.getSessionID(), txn.getOrderId());

        return new UnisusWebhookResponse(
                0,
                "Success",
                txn.getOrderId()
        );
    }

    private UnisusWebhookResponse handlePgResponse(
            UnisusWebhookRequest request) {

        String trnRefNo =
                request.getTransaction().getTrnRefNo();

        log.info("Processing PG Response webhook. trnRefNo={}, sessionId={}",
                trnRefNo, request.getSessionID());

        try {
            log.info("Triggering async processing for trnRefNo={}", trnRefNo);
            asyncProcessor.processAsync(request);
            log.info("Async processing triggered successfully for trnRefNo={}", trnRefNo);

        } catch (Exception e) {
            log.error("Error triggering async processing for trnRefNo={}",
                    trnRefNo, e);
            // DO NOT THROW
        }

        log.info("Returning success response to Unisus for trnRefNo={}", trnRefNo);

        return new UnisusWebhookResponse(
                0,
                "Success",
                trnRefNo
        );
    }

}