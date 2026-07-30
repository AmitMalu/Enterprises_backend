package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.NixaSoftPayoutRequest;
import com.im.admin.request.NixaSoftPayoutStatusRequest;
import com.im.admin.request.NixaSoftPayoutStatusResponse;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.*;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
@LoggableVendor
public class NixaSoftPayoutServiceImpl implements PayoutVendorService, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public String getVendorName() {
        return "NixaSoft Payout";
    }

    @Override
    public PayoutResponseDto sendPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        log.info("Starting NixaSoft payout | orderId={} | amount={}",
                orderId,
                req.getAmount());

        try {

            // VALIDATE BENEFICIARY
            Optional<BeneficiaryBankAccount> beneficiaryBankAccount =
                    beneficiaryBankAccountRepository.findTopByRetailerIdAndAccountNumberOrderByIdDesc(req.getRetailerId(), req.getAccountNumber());

            if (beneficiaryBankAccount.isEmpty()) {
                return failed(orderId, "Beneficiary bank details not found");
            }

            // AES FLOW
            boolean isAesFlow =
                    "AES".equalsIgnoreCase(creds.getTokenType());

            return isAesFlow
                    ? processAesPayout(
                    req,
                    creds,
                    orderId,
                    requestHeaderMetaDto
            )
                    : processDirectVendorPayout(
                    req,
                    creds,
                    orderId,
                    beneficiaryBankAccount,
                    requestHeaderMetaDto
            );

        }

        // VENDOR API ERRORS
        catch (HttpStatusCodeException ex) {

            String vendorResponse = ex.getResponseBodyAsString();

            log.error("Vendor API Error | orderId={} | response={}",
                    orderId,
                    vendorResponse);

            return failed(
                    orderId,
                    vendorResponse
            );
        }

        // INTERNAL ERRORS
        catch (Exception e) {

            Throwable root = getRootCause(e);

            log.error("Internal payout error | orderId={} | error={}",
                    orderId,
                    root.getMessage(),
                    e);

            return failed(
                    orderId,
                    root.getMessage()
            );
        }
    }


/* =====================================================
   COMMON FAILED RESPONSE
   ===================================================== */

    private PayoutResponseDto failed(
            String orderId,
            String message) {

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .status("FAILED")
                .message(
                        message != null
                                ? message
                                : "Something went wrong"
                )
                .build();
    }


/* =====================================================
   ROOT CAUSE EXTRACTOR
   ===================================================== */

    private Throwable getRootCause(Throwable throwable) {

        Throwable root = throwable;

        while (root.getCause() != null
                && root.getCause() != root) {

            root = root.getCause();
        }

        return root;
    }

    //   AES PAYOUT FLOW
    private PayoutResponseDto processAesPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String baseTxnId,
            RequestHeaderMetaDto requestHeaderMetaDto) throws Exception {

        String token = commonMethods.generateTokenForAES(creds);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", creds.getClientId());
        headers.set("token", token);
        headers.setBearerAuth(token);

        headers.set("latitude", String.valueOf(requestHeaderMetaDto.getLatitude()));
        headers.set("longitude", String.valueOf(requestHeaderMetaDto.getLongitude()));

       String orderId = baseTxnId.startsWith("TXN") ? baseTxnId.substring(3) : baseTxnId;

        SendPayoutRequest payoutRequest = new SendPayoutRequest();

        payoutRequest.setAmount(req.getAmount());
        payoutRequest.setOrderId(orderId);
        payoutRequest.setIfscCode(req.getIfscCode());
        payoutRequest.setAccountNumber(req.getAccountNumber());
        payoutRequest.setBankName(req.getBankName());
        payoutRequest.setPaymentMode(req.getPaymentMode());
        payoutRequest.setBeneficiaryName(req.getBeneficiaryName());
        payoutRequest.setPartnerId(creds.getUsername());

        HttpEntity<SendPayoutRequest> requestEntity =
                new HttpEntity<>(payoutRequest, headers);

        //String baseUrl = creds.getBaseUrlUat();
        String prodUrl = creds.getBaseUrlProd();

        log.info("Calling AES payout API | url={}", prodUrl);

        ResponseEntity<CommonResponse<PayoutResponseDto>> response =
                restTemplate.exchange(
                        prodUrl + "/api/send-payout",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<CommonResponse<PayoutResponseDto>>() {
                        }
                );

        CommonResponse<PayoutResponseDto> responseBody = response.getBody();

        log.info("AES payout response => {}", responseBody);

        if (responseBody == null || responseBody.getData() == null) {
            return buildFailedResponse("TXN" + baseTxnId, "Empty response from AES vendor");
        }

        log.info("AES payout completed | orderId={} | status={}",
                "TXN" + baseTxnId,
                responseBody.getData().getStatus());

        // SAVE PENDING TRANSACTION

        PayoutPendingTransactions pending = new PayoutPendingTransactions();

        pending.setTransactionId(orderId);
        pending.setSchemeCode(req.getSchemeCode());
        pending.setRetailerId(req.getRetailerId());
        pending.setAmount(req.getAmount());
        pending.setTransactionType(req.getPaymentMode());
        pending.setStatus("PENDING");
        pending.setCreatedAt(LocalDateTime.now());
        pending.setIfscCode(req.getIfscCode());
        pending.setAccountNumber(req.getAccountNumber());
        pending.setBeneficiaryName(req.getBeneficiaryName());
        pending.setLatitude(requestHeaderMetaDto.getLatitude());
        pending.setLongitude(requestHeaderMetaDto.getLongitude());

        payoutPendingTransactionRepository.save(pending);

        return PayoutResponseDto.builder()
                .orderId("TXN" + baseTxnId)
                .status(responseBody.getData().getStatus())
                .amount(req.getAmount())
                .message(responseBody.getMessage())
                .build();
    }

//   DIRECT NIXASOFT PAYOUT FLOW

    private PayoutResponseDto processDirectVendorPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            Optional<BeneficiaryBankAccount> beneficiaryBankAccount,
            RequestHeaderMetaDto requestHeaderMetaDto
    ) {

        Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Retailer not found: " + req.getRetailerId()
                        )
                );

        /*
         * Save transaction BEFORE calling vendor.
         *
         * This ensures that even when vendor API times out,
         * the transaction is available in the pending table.
         */
        PayoutPendingTransactions pending = new PayoutPendingTransactions();

        pending.setTransactionId(orderId);
        pending.setSchemeCode(req.getSchemeCode());
        pending.setRetailerId(req.getRetailerId());
        pending.setAmount(req.getAmount());
        pending.setTransactionType(req.getPaymentMode());
        pending.setStatus("PENDING");
        pending.setCreatedAt(LocalDateTime.now());
        pending.setIfscCode(req.getIfscCode());
        pending.setAccountNumber(req.getAccountNumber());
        pending.setBeneficiaryName(req.getBeneficiaryName());
        pending.setLatitude(requestHeaderMetaDto.getLatitude());
        pending.setLongitude(requestHeaderMetaDto.getLongitude());
        pending.setBankName(beneficiaryBankAccount.get().getBankName());

        pending = payoutPendingTransactionRepository.saveAndFlush(pending);

        log.info(
                "Pending payout transaction saved before vendor call | orderId={} | retailerId={} | amount={}",
                orderId,
                req.getRetailerId(),
                req.getAmount()
        );

        try {

            // REQUEST BODY

            NixaSoftPayoutRequest requestBody = new NixaSoftPayoutRequest();

            requestBody.setAmount(req.getAmount().toPlainString());
            requestBody.setMobileNumber(retailer.getMobileNumber());
            requestBody.setRequestId(orderId);
            requestBody.setAccountNumber(req.getAccountNumber());
            requestBody.setIfscCode(req.getIfscCode());
            requestBody.setBeneficiaryName(req.getBeneficiaryName());
            requestBody.setBankName(beneficiaryBankAccount.get().getBankName());
            requestBody.setTransferMode(req.getPaymentMode());
            requestBody.setEmailId(retailer.getEmailId());
            requestBody.setLatitude(
                    requestHeaderMetaDto.getLatitude().toString()
            );
            requestBody.setLongitude(
                    requestHeaderMetaDto.getLongitude().toString()
            );

            // HEADERS

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", creds.getToken());

            HttpEntity<NixaSoftPayoutRequest> entity =
                    new HttpEntity<>(requestBody, headers);

            log.info(
                    "Calling NixaSoft payout API | orderId={} | request={}",
                    orderId,
                    objectMapper.writeValueAsString(requestBody)
            );

            ResponseEntity<NixaSoftPayoutResponse> response =
                    restTemplate.exchange(
                            creds.getBaseUrlProd() + "/payout",
                            HttpMethod.POST,
                            entity,
                            NixaSoftPayoutResponse.class
                    );

            NixaSoftPayoutResponse body = response.getBody();

            if (body == null) {

                log.error(
                        "Empty response received from NixaSoft | orderId={}",
                        orderId
                );

                // Keep transaction as PENDING because vendor result is unknown
                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .status("PENDING")
                        .amount(req.getAmount())
                        .message(
                                "Empty response from vendor. Transaction is under processing."
                        )
                        .build();
            }

            log.info(
                    "NixaSoft payout response | orderId={} | statusCode={} | message={}",
                    orderId,
                    body.getStatuscode(),
                    body.getMessage()
            );

            String status = mapVendorStatus(body.getStatuscode());

            /*
             * Update pending-table status based on vendor response.
             * Keep it PENDING when vendor status mapping returns PENDING.
             */
            pending.setStatus(status);

            payoutPendingTransactionRepository.saveAndFlush(pending);

            log.info(
                    "Payout transaction updated successfully | orderId={} | status={}",
                    orderId,
                    status
            );

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status(status)
                    .amount(req.getAmount())
                    .message(body.getMessage())
                    .build();

        } catch (ResourceAccessException ex) {

            /*
             * Timeout or connection error.
             *
             * Do not mark it FAILED because the request might have reached
             * the vendor and only the response could have timed out.
             * Keep it PENDING for status enquiry/reconciliation.
             */
            log.error(
                    "NixaSoft connection timeout/error | orderId={} | error={}",
                    orderId,
                    ex.getMessage(),
                    ex
            );

            pending.setStatus("PENDING");
            payoutPendingTransactionRepository.saveAndFlush(pending);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status("PENDING")
                    .amount(req.getAmount())
                    .message(
                            "Vendor connection timed out. Transaction is under processing."
                    )
                    .build();

        } catch (HttpStatusCodeException ex) {

            log.error(
                    "NixaSoft HTTP error | orderId={} | httpStatus={} | response={}",
                    orderId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );

            /*
             * You may use FAILED here because the vendor returned
             * an actual HTTP error response.
             */
            pending.setStatus("FAILED");
            payoutPendingTransactionRepository.saveAndFlush(pending);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .amount(req.getAmount())
                    .message(
                            "Vendor returned HTTP error: "
                                    + ex.getStatusCode().value()
                    )
                    .build();

        } catch (Exception ex) {

            log.error(
                    "Unexpected NixaSoft payout error | orderId={} | error={}",
                    orderId,
                    ex.getMessage(),
                    ex
            );

            /*
             * Keep unknown errors pending unless you are certain
             * that the vendor did not process the transaction.
             */
            pending.setStatus("PENDING");
            payoutPendingTransactionRepository.saveAndFlush(pending);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .status("PENDING")
                    .amount(req.getAmount())
                    .message(
                            "Unable to confirm vendor response. Transaction is under processing."
                    )
                    .build();
        }
    }

    private String mapVendorStatus(String vendorStatus) {

        return switch (vendorStatus) {
            case "TXN" -> "SUCCESS";
            case "TXP" -> "PENDING";
            default -> "FAILED";
        };
    }

    private PayoutResponseDto buildFailedResponse(
            String orderId,
            String message) {

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .status("FAILED")
                .message(message != null ? message : "Unknown Error")
                .build();
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("NixaSoft Callback Received: {}", payload);

        // ===== 1. Extract Fields =====
        String status = payload.get("status");
        String type = payload.get("type");
        String orderId = payload.get("requestId");
        String vendorTxnId = payload.get("orderId");
        String amount = payload.get("amount");
        String utr = payload.get("utr");
        String description = payload.get("description");

        log.info("[1] Parsed Callback: orderId={}, requestId={}, status={}, amount={}, utr={}",
                vendorTxnId, orderId, status, amount, utr);

        // ===== 2. Fetch Pending Transaction =====
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                .findByTransactionId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Pending transaction not found for order ID: " + orderId));

        // ===== Already SUCCESS check =====
        if ("SUCCESS".equalsIgnoreCase(pendingTxn.getStatus())) {
            log.warn("Transaction already marked SUCCESS for orderId={}", orderId);
            return "Transaction already processed successfully";
        }

        log.info("[2] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                pendingTxn.getAmount(), pendingTxn.getStatus());

        // Sanitize amount from callback payload (remove commas)
        String cleanCallbackAmount = amount != null ? amount.replace(",", "") : "0";
        Double txnAmt = Double.valueOf(cleanCallbackAmount);

        // ===== 3. Fetch Scheme Rate =====
        log.info("[3] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

        SchemeRate selectedScheme = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        pendingTxn.getSchemeCode(),
                        pendingTxn.getTransactionType(),
                        txnAmt)
                .orElseThrow(() ->
                        new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

        log.info("[3.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                selectedScheme.getGross(), selectedScheme.getRetailer());

        // ===== 4. Fetch Scheme =====
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() ->
                        new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

        log.info("[4] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

        // ===== 5. Extract Product & SubProduct =====
        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

        if (hashParts.length < 2) {
            throw new RuntimeException("Invalid schemeCombinationHashCode format: "
                    + scheme.getSchemeCombinationHashCode());
        }

        short productId = Short.parseShort(hashParts[0]);
        String subProductIdStr = hashParts[1];

        Products product = productsRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + productId));

        SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                .orElseThrow(() ->
                        new RuntimeException("SubProduct not found with id: " + subProductIdStr));

        log.info("[5] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                product.getName(), subProduct.getSubProductTitle());

        // ===== 6. Prepare DTO =====
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
        dto.setOperatorName(pendingTxn.getTransactionType());
        dto.setOperatorCode(selectedScheme.getOperatorCode());
        dto.setSchemeRate(selectedScheme);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(pendingTxn.getRetailerId());
        dto.setVendorName("Nixasoft Payout");
        dto.setVendorTxnId(utr);

        log.info("[6] DTO ready for transaction processing: {}", dto);

        // ===== 7. Map Vendor Status =====
        String finalStatus = switch (status) {

            case "SUCCESS" -> "SUCCESS";

            case "FAILED" -> "FAILED";

            case "PENDING" -> "PENDING";

            default -> "UNKNOWN";
        };

        log.info("[7] Status mapped from vendor: {} -> {}", status, finalStatus);

        // ===== 8. Process Transaction =====
        transactionEngineServiceImpl.processTransaction(dto, finalStatus);

        log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                orderId, finalStatus);

        // ===== 9. Update UTR =====
        pendingTxn.setStatus(finalStatus);
        payoutPendingTransactionRepository.save(pendingTxn);

        log.info("[9] Pending transaction updated with UTR={}, STATUS={}", utr, finalStatus);

        return "SUCCESS";
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {

        log.info("Checking NixaSoft payout status for orderId={}", orderId);

        try {

            // ===== Request Body =====
            NixaSoftPayoutStatusRequest statusRequest = new NixaSoftPayoutStatusRequest();
            statusRequest.setRequestId(orderId);

            // ===== Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", credentials.getToken());

            HttpEntity<NixaSoftPayoutStatusRequest> entity =
                    new HttpEntity<>(statusRequest, headers);

            // ===== Call API =====
            ResponseEntity<NixaSoftPayoutStatusResponse> response =
                    restTemplate.exchange(
                            credentials.getBaseUrlUat() + "/check-status",
                            HttpMethod.POST,
                            entity,
                            NixaSoftPayoutStatusResponse.class
                    );

            NixaSoftPayoutStatusResponse body = response.getBody();

            log.info("NixaSoft Status Response: {}", body);

            // ===== Map Status =====
            assert body != null;
            String mappedStatus = switch (body.getStatus()) {
                case "SUCCESS" -> "SUCCESS";
                case "PENDING" -> "PENDING";
                case "FAILED" -> "FAILED";
                default -> "UNKNOWN";
            };

            // ===== Build Response =====
            PayoutStatusResponse payoutStatusResponse = new PayoutStatusResponse();

            payoutStatusResponse.setOrderId(body.getOrderId());
            payoutStatusResponse.setStatus(mappedStatus);
            payoutStatusResponse.setMessage(body.getDescription());

            return payoutStatusResponse;

        } catch (Exception e) {

            log.error("Error while checking NixaSoft payout status for orderId={}", orderId, e);

            PayoutStatusResponse response = new PayoutStatusResponse();
            response.setOrderId(orderId);
            response.setStatus("FAILED");
            response.setMessage("Vendor API Error");

            return response;
        }
    }
}
