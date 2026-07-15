package com.im.admin.paymentgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.PgTransactionRequestDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.impl.RunpaisaPGServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunPaisaInitiator implements PaymentGatewayInitiator {

    private final RunpaisaPGServiceImpl runPaisaServiceImpl;
    private final ObjectMapper objectMapper;

    @Override
    public String getVendorName() {
        return "Runpaisa Payment Gateway";
    }


    @Override
    public PgUnifiedOrderResponse initiate(
            PGCreateOrderCommand command,
            RequestHeaderMetaDto requestHeaderMetaDto) throws JsonProcessingException {

        log.info(
                "RUNPAISA initiate called. merchantRef={}, subProductId={}, amount={}, medium{}, ipAddress{} " ,
                command.getMerchantRef(),
                command.getSubProductId(),
                command.getAmount(),
                command.getMedium(),
                requestHeaderMetaDto.getIpAddress()
        );

        try {

            CreateOrderRequest req = new CreateOrderRequest();
            req.setInitiatedBy(command.getInitiatedBy());
            req.setProductId(command.getProductId());
            req.setSubProductId(command.getSubProductId());
            req.setAmount(command.getAmount());
            req.setMerchantRef(command.getMerchantRef());
            req.setCardBinNo(command.getCardBinNo());
            req.setUsername(command.getUsername());
            req.setCallbackUrl(command.getCallbackUrl());
            req.setTransactionScheme(command.getSchemeDetails());
            req.setMedium(requestHeaderMetaDto.getMedium());
            req.setClientIp(requestHeaderMetaDto.getIpAddress());
            req.setRequestDomainName(requestHeaderMetaDto.getRequestDomainName());

            // ===== LOG REQUEST BEFORE CREATE ORDER CALL =====
            log.info("=== PREPARING TO CALL RUNPAISA CREATE ORDER ===");
            log.info("CreateOrderRequest prepared with:");
            log.info("  - Amount: {}", req.getAmount());
            log.info("  - SubProductId: {}", req.getSubProductId());
            log.info("  - InitiatedBy: {}", req.getInitiatedBy());

            // Log transaction scheme if present
            if (req.getTransactionScheme() != null) {
                log.debug("TransactionScheme: {}", req.getTransactionScheme());
            }

            // Log complete request JSON for debugging
            try {
                String requestJson = objectMapper.writeValueAsString(req);
                log.debug("Complete Request JSON: {}", requestJson);
            } catch (Exception e) {
                log.warn("Unable to serialize request to JSON: {}", e.getMessage());
            }

            log.info("Calling RunPaisa createOrder service...");
            long startTime = System.currentTimeMillis();

            ResponseEntity<CommonResponse<String>> response =
                    runPaisaServiceImpl.createOrder(req, requestHeaderMetaDto);

            if (response == null) {
                throw new RuntimeException("RunPaisa returned null response");
            }

            CommonResponse<String> body = response.getBody();

            if (body == null) {
                throw new RuntimeException(
                        "RunPaisa returned empty body"
                );
            }

            String rawJson = body.getData();

            log.debug("RunPaisa raw response: {}", rawJson);

            if (rawJson == null || rawJson.isBlank()) {

                log.error(
                        "RunPaisa returned empty data. message={}", body.getMessage()
                );

                throw new RuntimeException(
                        "RunPaisa returned empty data"
                );
            }

            JsonNode json = objectMapper.readTree(rawJson);

            PgUnifiedOrderResponse unifiedResponse =
                    PgUnifiedOrderResponse.builder()
                            .vendor("RUNPAISA")
                            .orderId(json.path("order_id").asText(null))
                            .paymentLink(json.path("paymentLink").asText(null))
                            .orderToken(json.path("order_token").asText(null))
                            .status(json.path("order_status").asText(null))
                            .build();

            log.info(
                    "RUNPAISA initiate completed. merchantRef={}, orderId={}, status={}",
                    command.getMerchantRef(),
                    unifiedResponse.getOrderId(),
                    unifiedResponse.getStatus()
            );

            return unifiedResponse;

        } catch (Exception e) {

            log.error(
                    "RUNPAISA initiate failed. merchantRef={}",
                    command.getMerchantRef(),
                    e
            );

            throw e;
        }
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {

        log.info("RUNPAISA checkStatus called. orderId={}", orderId);

        try {
            PgStatusResponse response =
                    runPaisaServiceImpl.orderStatus(orderId);

            log.info("RUNPAISA checkStatus completed. orderId={}, httpStatus={}",
                    orderId,
                    response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Error during RUNPAISA checkStatus. orderId={}", orderId, e);
            throw e;
        }
    }


    @Override
    public CommonResponse<String> validate(PgTransactionRequestDto request) {
        log.info("RUNPAISA validate called. amount={}", request.getAmount());

        try {

            CommonResponse<String> response =
                    runPaisaServiceImpl.validateTransaction(request);

            log.info("RUNPAISA validate completed.");

            return response;

        } catch (Exception e) {
            log.error("Error during RUNPAISA validate.", e);
            throw e;
        }
    }
}
