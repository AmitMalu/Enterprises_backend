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

        try {

            // ====== 1. Get Vendor & Credentials ======
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("No credentials found"));

            // ====== 2. Build Authenticate Request ======
            UnisusAuthRequest authRequest = new UnisusAuthRequest(
                    credentials.getClientId(),     // APIKey
                    credentials.getUsername(),     // MobileNo
                    credentials.getClientSecret()     // AgentID or MobileNo
            );

            log.info("Calling Unisus Authenticate API...");

            WebClient webClient = webClientBuilder.build();

            UnisusAuthResponse response = webClient.post()
                    .uri(credentials.getTokenUrlUat())
                    .bodyValue(authRequest)
                    .retrieve()
                    .bodyToMono(UnisusAuthResponse.class)
                    .block();

            if (response == null || response.getStatus() != 0) {
                throw new RuntimeException(
                        "Authentication failed: " + response.getStatusMsg());
            }

            // ====== 3. Build Redirect URL ======
            String redirectUrl = credentials.getBaseUrlUat() +
                    "?SessionID=" + response.getSessionId() +
                    "&AccessKey=" + response.getAccessKey();

            log.info("Unisus Redirect URL Created Successfully");

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

            return redirectUrl;

        } catch (Exception e) {
            log.error("Error during Unisus authentication", e);
            throw new RuntimeException("Unisus Authentication Failed");
        }
    }

    @Override
    public UnisusWebhookResponse processWebhook(
            UnisusWebhookRequest request) {

        return switch (request.getRequestType()) {
            case 15 -> handlePgRequest(request);
            case 16 -> handlePgResponse(request);
            //case 17 -> handlePgReverse(request);
            default -> new UnisusWebhookResponse(
                    1,
                    "Invalid Request Type",
                    null
            );
        };
    }

    private UnisusWebhookResponse handlePgRequest(
            UnisusWebhookRequest request) {

        PgTransactionMapping txn =
                pgTransactionMappingRepository
                        .findByVendorSessionId(String.valueOf(request.getSessionID()))
                        .orElseThrow(() -> new RuntimeException("Txn not found"));

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

        try {
            // Trigger async processing
            asyncProcessor.processAsync(request);
        } catch (Exception e) {
            log.error("Error triggering async processing", e);
            // DO NOT THROW
        }

        // Immediately return success to vendor
        return new UnisusWebhookResponse(
                0,
                "Success",
                trnRefNo
        );
    }

}