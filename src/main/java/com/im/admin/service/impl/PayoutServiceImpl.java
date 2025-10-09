package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.callbacks.RunpaisaPayoutCallback;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.controller.AESController;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.RunPaisaCallback;
import com.im.admin.service.PayoutService;
import com.im.admin.service.VendorCallbackParser;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class PayoutServiceImpl implements PayoutService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private AESController aesController;

    @Autowired
    private TransactionServiceImpl transactionServiceImpl;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private CallbackParserFactory callbackParserFactory;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ProductsRepository productsRepository;

    RestTemplate restTemplate = new RestTemplate();

    ObjectMapper mapper = new ObjectMapper();

    @Value("${payout.base.url.uat}")
    private String baseUrl;

    @Value("${payout.client_id}")
    private String clientId;

    @Value("${payout.client_secret}")
    private String clientSecret;

    @Override
    public String generateToken() {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_secret", clientSecret);
        headers.set("client_id", clientId);

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = baseUrl + "/token";
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PayoutTokenResponseDTO> response = restTemplate.postForEntity(
                url,
                requestEntity,
                PayoutTokenResponseDTO.class
        );

        PayoutTokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed To Get Token From Payout Vendor");
        }
    }

    public String generateTokenForPayout(VendorCredentials vendorCredentials) {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", vendorCredentials.getClientId());
        headers.set("client_secret", vendorCredentials.getClientSecret());
        //headers.set("username", vendorCredentials.getUsername());
        //headers.set("password", vendorCredentials.getPassword());

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = vendorCredentials.getTokenUrlProd();
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PayoutTokenResponseDTO> response = restTemplate.postForEntity(
                url,
                requestEntity,
                PayoutTokenResponseDTO.class
        );

        PayoutTokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed To Get Token From Payout Vendor");
        }
    }

    @Override
    @Transactional
    public CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest) {
        try {

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            Optional<SubProducts> subProducts = subProductsRepository.findById(sendPayoutRequest.getSubProductId());
            WalletType walletType = WalletType.valueOf(subProducts.get().getWalletType());

            BigDecimal availableBalance = commonMethods.getCurrentBalance(sendPayoutRequest.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(sendPayoutRequest.getAmount()) < 0) {
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(sendPayoutRequest.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            log.debug("Vendor credentials retrieved: {}", credentials.getVendor().getVendorName());

            // ====== Check Vendor Type ======
            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {
                // =========================
                // AES Flow (Admin Panel)
                // =========================
                String token = generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS

                SendPayoutRequest payoutJsonRequestBody = new SendPayoutRequest();

                payoutJsonRequestBody.setAccountNumber(sendPayoutRequest.getAccountNumber());
                payoutJsonRequestBody.setIfscCode(sendPayoutRequest.getIfscCode());
                payoutJsonRequestBody.setAmount(sendPayoutRequest.getAmount());
                payoutJsonRequestBody.setOrderId("TXN" + baseTransactionId);
                payoutJsonRequestBody.setBeneficiaryName(sendPayoutRequest.getBeneficiaryName());
                payoutJsonRequestBody.setPaymentMode(sendPayoutRequest.getPaymentMode());
                payoutJsonRequestBody.setCallbackurl(sendPayoutRequest.getCallbackurl());

                HttpEntity<SendPayoutRequest> requestEntity = new HttpEntity<>(payoutJsonRequestBody, headers);

                String url = credentials.getBaseUrlUat() + "/payment";
                log.info("Sending payout request to RunPaisa. URL: {}, Headers: {}, Body: {}", url, requestEntity, String.class);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

            } else {

                AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), sendPayoutRequest.getRetailerId());
                commonMethods.updateWallet(walletAmountTable, sendPayoutRequest.getRetailerId(), walletType, sendPayoutRequest.getAmount(), "Debit", "TXN" + baseTransactionId, "VENDOR_PENDING", "DEBIT");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", generateTokenForPayout(credentials));

                Map<String, Object> payout = new LinkedHashMap<>();
                payout.put("account_number", String.valueOf(sendPayoutRequest.getAccountNumber()));  // "026291800001191"
                payout.put("ifsc_code", String.valueOf(sendPayoutRequest.getIfscCode()));            // "YESB0000001"
                payout.put("amount", String.format("%.2f", sendPayoutRequest.getAmount()));          // "11.00"
                payout.put("order_id", "TXN" + baseTransactionId);                                   // "TXN25082012270100272"
                payout.put("beneficiary_name", String.valueOf(sendPayoutRequest.getBeneficiaryName()));
                payout.put("payment_mode", String.valueOf(sendPayoutRequest.getPaymentMode()));      // "IMPS"

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payout, headers);

                String url = credentials.getBaseUrlUat() + "/payment";
                log.info("Sending payout request to RunPaisa. URL: {}, Headers: {}, Body: {}", url, headers, payout);

                ResponseEntity<PayoutResponseDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        PayoutResponseDto.class
                );

                PayoutResponseDto payoutResponse = response.getBody();
                assert payoutResponse != null;
                log.info("RunPaisa payout response: {}", payoutResponse.getMessage());

                if (payoutResponse != null && "ACCEPTED".equalsIgnoreCase(payoutResponse.getStatus())) {
                    return new CommonResponse<>(200, payoutResponse.getMessage(), payoutResponse);
                } else {
                    String errorMsg = payoutResponse != null ? payoutResponse.getMessage() : "Null response from vendor";
                    return new CommonResponse<>(400, errorMsg, payoutResponse);
                }
            }

        } catch (HttpStatusCodeException e) {
            log.error("Vendor returned HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new CommonResponse<>(e.getStatusCode().value(), "Vendor Error", null);
        } catch (RestClientException e) {
            log.error("Vendor communication error: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Vendor communication failed", null);
        } catch (Exception e) {
            log.error("General exception: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Exception occurred during payout", null);
        }
        return new CommonResponse<>(500, "Something went wrong", null);

    }

    private String generateTokenForAES(VendorCredentials vendorCredentials) {
        // Call AES encrypt service
        ResponseEntity<?> responseEntity = aesController.encryptCredentials(
                new EncryptDecryptCredentialsDto(vendorCredentials.getUsername(), vendorCredentials.getPassword())
        );

        @SuppressWarnings("unchecked")
        Map<String, String> encryptedMap = (Map<String, String>) responseEntity.getBody();
        String encUser = encryptedMap.get("cipherUsername");
        String encPass = encryptedMap.get("cipherPassword");
        String salt = encryptedMap.get("salt");
        String iv = encryptedMap.get("iv");

        // Build JSON for token request
        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("username", encUser);
        jsonBody.put("password", encPass);
        jsonBody.put("client_id", vendorCredentials.getClientId());
        jsonBody.put("salt", salt);
        jsonBody.put("ivBase64", iv);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(vendorCredentials.getTokenUrlUat(), requestEntity, Map.class);
        Map<String, Object> responseBodyMap = response.getBody();
        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
        }

        return (String) responseBodyMap.get("token");
    }


    @Override
    public PayoutMerchantCallbackResponse updateMerchantCallbackUrl(SendPayoutRequest request) {
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", generateTokenForPayout(credentials));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("callback_url", credentials.getCallbackUrl());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Update Merchant Callback Url: headers={}, body={}", headers, body);

        ResponseEntity<PayoutMerchantCallbackResponse> response = restTemplate.exchange(
                baseUrl + "/updatemerchantcallback",
                HttpMethod.POST,
                requestEntity,
                PayoutMerchantCallbackResponse.class
        );

        return response.getBody();
    }

    @Override
    public String handlePayoutCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {

        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("---- [PAYOUT CALLBACK] Payload received: {}", payload);

        // Deserialize the raw payload into the payout DTO
        RunpaisaPayoutCallback payoutCallback = mapper.convertValue(payload, RunpaisaPayoutCallback.class);

        log.info("---- [PAYOUT CALLBACK] Parsed DTO: {}", payoutCallback);

        // Extract the partner_json (nested transaction info)
        String partnerJsonStr = payoutCallback.getTxn_method().getPartner_json();
        Map<String, Object> partnerJson = mapper.readValue(partnerJsonStr, new TypeReference<>() {});

        log.info("---- [PAYOUT CALLBACK] Partner JSON: {}", partnerJson);

        // Convert partner_json to your existing RunPaisaCallback (PG-style) DTO
        RunPaisaCallback callback = convertPartnerJsonToRunPaisaCallback(partnerJson);

        // Optional: add some mapping from payout top-level
        callback.setStatusCode(callback.getStatusCode());
        callback.setOrderId(payoutCallback.getRef_id());
        callback.setPgPartner(payoutCallback.getPartner());
        callback.setCardType(payoutCallback.getTxn_method().getCard_type());
        callback.setCardNumber(payoutCallback.getTxn_method().getCard_number());
        callback.setTxnMode(payoutCallback.getTxn_method().getMode());

        // Process as before
        processCallback(callback);

        // Save callback event log
        try {
            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp,
                    "Payout callback processed successfully", LocalDateTime.now());
            log.info("---- [PAYOUT CALLBACK] Successfully logged callback event");
            return "Payout callback processed successfully";
        } catch (Exception e) {
            log.error("---- [PAYOUT CALLBACK] Error while saving log: {}", e.getMessage(), e);
            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp, e.getMessage(), LocalDateTime.now());
            throw new RuntimeException("Payout callback processing failed");
        }
    }

    private RunPaisaCallback convertPartnerJsonToRunPaisaCallback(Map<String, Object> partnerJson) {
        RunPaisaCallback callback = new RunPaisaCallback();

        callback.setStatus((String) partnerJson.get("STATUS"));
        callback.setStatusCode((String) partnerJson.get("STATUS_CODE"));
        callback.setOrderId((String) partnerJson.get("ORDER_ID"));
        callback.setTxnMode((String) partnerJson.get("TXN_MODE"));
        callback.setTxnAmount(String.valueOf(partnerJson.get("TXN_AMOUNT")));
        callback.setCardCategory((String) partnerJson.get("CARD_CATEGORY"));
        callback.setTxnDate((String) partnerJson.get("TXN_DATE"));
        callback.setTxnInfo((String) partnerJson.get("TXN_INFO"));
        callback.setCustomerName((String) partnerJson.get("CUSTOMER_NAME"));
        callback.setCustomerEmail((String) partnerJson.get("CUSTOMER_EMAIL"));
        callback.setCustomerPhone((String) partnerJson.get("CUSTOMER_PHONE"));
        callback.setBankTxnId((String) partnerJson.get("BANK_TXNID"));
        callback.setBankCode((String) partnerJson.get("BANK_CODE"));
        callback.setErrorId((String) partnerJson.get("ERROR_ID"));
        callback.setErrorDesc((String) partnerJson.get("ERROR_DESC"));
        callback.setCardNumber((String) partnerJson.get("CARD_NUMBER"));
        callback.setCardType((String) partnerJson.get("CARD_TYPE"));
        callback.setUnmappedStatus((String) partnerJson.get("UNMAPPED_STATUS"));
        callback.setPgPartner((String) partnerJson.get("PG_PARTNER"));
        callback.setMercUnqRef((String) partnerJson.get("MERC_UNQ_REF"));

        log.info("---- [PAYOUT CALLBACK] Converted partner_json to RunPaisaCallback: {}", callback);
        return callback;
    }


    @Override
    public CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto) {

        // Count existing records
        long count = beneficiaryBankAccountRepository.count();

        //Check limit (max 200)
        if (count >= 200) {
            return CommonResponse.handleErrors("Maximum limit of 200 beneficiary records reached. Cannot add more.");
        }

        //   Map DTO to Entity
        BeneficiaryBankAccount entity = new BeneficiaryBankAccount();
        entity.setRetailerId(dto.getRetailerId());
        entity.setBankName(dto.getBankName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setCreatedBy(dto.getCreatedBy());

        //   Save entity
        BeneficiaryBankAccount saved = beneficiaryBankAccountRepository.save(entity);

        //   Return success response
        return CommonResponse.success(saved, "Bank account saved successfully");
    }


    @Override
    public CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids) {
        List<Long> notFound = new ArrayList<>();

        for (Long id : ids) {
            if (beneficiaryBankAccountRepository.existsById(id)) {
                beneficiaryBankAccountRepository.deleteById(id);
            } else {
                notFound.add(id);
            }
        }

        String message = "Deleted successfully.";
        if (!notFound.isEmpty()) {
            message += " But some IDs not found: " + notFound;
        }

        return CommonResponse.success(message, "Processed IDs: " + ids);
    }

    @Override
    public CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(Long id, String retailerId, String bankName, String bankHolderName, String accountNumber, String ifscCode, String paymentMode, Boolean isVerified, LocalDate createdOn, String createdBy, Pageable pageable) {

        Page<BeneficiaryBankAccount> results =
                beneficiaryBankAccountRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (id != null) {
                        predicates.add(cb.equal(root.get("id"), id));
                    }
                    if (retailerId != null && !retailerId.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("retailerId")), "%" + retailerId.toLowerCase() + "%"));
                    }
                    if (bankName != null && !bankName.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
                    }
                    if (bankHolderName != null && !bankHolderName.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("bankHolderName")), "%" + bankHolderName.toLowerCase() + "%"));
                    }
                    if (accountNumber != null && !accountNumber.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("accountNumber")), "%" + accountNumber.toLowerCase() + "%"));
                    }
                    if (ifscCode != null && !ifscCode.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("ifscCode")), "%" + ifscCode.toLowerCase() + "%"));
                    }
                    if (paymentMode != null && !paymentMode.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("paymentMode")), "%" + paymentMode.toLowerCase() + "%"));
                    }
                    if (isVerified != null) {
                        predicates.add(cb.equal(root.get("isVerified"), isVerified));
                    }
                    if (createdOn != null) {
                        predicates.add(cb.equal(cb.function("DATE", LocalDate.class, root.get("createdOn")), createdOn));
                    }
                    if (createdBy != null && !createdBy.isBlank()) {
                        predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                }, pageable);

        return CommonResponse.success(results, "Filtered results");
    }

    @Override
    public CommonResponse<BeneficiaryBankAccountDTO> updateBeneficiary(Long id, BeneficiaryBankAccountDTO requestDto) {
        Optional<BeneficiaryBankAccount> optional = beneficiaryBankAccountRepository.findById(id);

        if (optional.isEmpty()) {
            return CommonResponse.error(HttpStatus.BAD_REQUEST.value(),
                    "Beneficiary Bank Account not found for ID: " + id);
        }


        BeneficiaryBankAccount entity = optional.get();

        // Update fields
        if (requestDto.getRetailerId() != null) entity.setRetailerId(requestDto.getRetailerId());
        if (requestDto.getBankName() != null) entity.setBankName(requestDto.getBankName());
        if (requestDto.getBankHolderName() != null) entity.setBankHolderName(requestDto.getBankHolderName());
        if (requestDto.getAccountNumber() != null) entity.setAccountNumber(requestDto.getAccountNumber());
        if (requestDto.getIfscCode() != null) entity.setIfscCode(requestDto.getIfscCode());
        if (requestDto.getPaymentMode() != null) entity.setPaymentMode(requestDto.getPaymentMode());
        if (requestDto.getIsVerified() != null) entity.setIsVerified(requestDto.getIsVerified());
        if (requestDto.getCreatedBy() != null) entity.setCreatedBy(requestDto.getCreatedBy());

        beneficiaryBankAccountRepository.save(entity);

        // Convert updated entity to DTO
        BeneficiaryBankAccountDTO responseDto = new BeneficiaryBankAccountDTO(
                entity.getId(),
                entity.getRetailerId(),
                entity.getBankName(),
                entity.getBankHolderName(),
                entity.getAccountNumber(),
                entity.getIfscCode(),
                entity.getPaymentMode(),
                entity.getIsVerified(),
                entity.getCreatedBy()
        );

        return CommonResponse.success(responseDto, "Beneficiary Bank Account updated successfully");
    }

    //@Override
    public void processCallback(RunPaisaCallback callback) {
        log.info("---- [Callback] Processing for Order ID: {}, Status: {}", callback.getOrderId(), callback.getStatus());

        TransactionSchemeDetailsRequestDto dto = null;

        if ("SUCCESS".equalsIgnoreCase(callback.getStatus())) {
            log.info("---- [Callback] Status is SUCCESS. Fetching saved transaction data...");

            // Construct operatorName (e.g., dth_tatasky)
            String callbackOperatorName = callback.getCardType().toLowerCase().replace(" ", "_") + "_" + callback.getPgPartner().toLowerCase();

            Double txnAmt = Double.valueOf(callback.getTxnAmount());

            List<SchemeRate> matchingSchemes = schemeRateRepository.findSchemeByOperatorName(callbackOperatorName);

            if (!matchingSchemes.isEmpty()) {
                SchemeRate selectedScheme = null;
                for (SchemeRate scheme : matchingSchemes) {
                    if (scheme.getSlabType() == SlabType.FIXED && Objects.equals(scheme.getFixedSlabValue(), txnAmt)) {
                        selectedScheme = scheme;
                        break;
                    } else if (scheme.getSlabType() == SlabType.VARIABLE && txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()) {
                        selectedScheme = scheme;
                        break;
                    }
                }

                if (selectedScheme != null) {
                    log.info("---- [Callback] Matching scheme found: {}", selectedScheme.getSchemeCode());

                    // 1. Fetch the Scheme using schemeCode
                    Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                            .orElseThrow(() -> new RuntimeException("Scheme Not Found"));

                    // 2. Parse schemeCombinationHashCode (e.g., 10-01-02-001-00001)
                    String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

                    short productId = Short.parseShort(hashParts[0]);
                    String subProductIdStr = hashParts[1];

                    // 3. Fetch Product and SubProduct
                    Products product = productsRepository.findById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

                    SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                            .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

                    // Fill DTO completely
                    dto = new TransactionSchemeDetailsRequestDto();
                    dto.setAmount(txnAmt);
                    dto.setConfirmAmount(txnAmt);
                    dto.setOperatorName(callbackOperatorName);
                    dto.setOperatorCode(selectedScheme.getOperatorCode());
                    dto.setSchemeRate(selectedScheme);
                    dto.setTransactionType(product.getTransactionType());
                    dto.setWalletType(subProduct.getWalletType());
                    dto.setGstValue(String.valueOf(product.getSgst()));
                    dto.setTdsRate(String.valueOf(product.getTds()));

                    dto.setHierarchyId(
                            userDetailsRepository.findByMobileNumber(callback.getCustomerPhone())
                                    .orElseThrow(() -> new RuntimeException("No User Found"))
                                    .getHierarchyId()
                    );

                    dto.setProductCode(product.getProductCode());
                    dto.setSubProductCode(subProduct.getSubProductCode());

                    dto.setIsCommission(product.getIsCommissions());
                    dto.setHasCharge(product.getHasCharges());
                } else {
                    log.warn("---- [Callback] No matching scheme found for operatorName: {}", callbackOperatorName);
                }
            } else {
                log.warn("---- [Callback] No schemes configured for operatorName: {}", callbackOperatorName);
            }

            log.info("---- [Callback] Deserialized Transaction DTO: {}", dto);
            assert dto != null;
            transactionServiceImpl.processTransaction(dto, callback.getStatus());

        } else {
            log.warn("---- [Callback] Payment FAILED or PENDING for Order ID: {}", callback.getOrderId());
            transactionServiceImpl.processTransaction(dto, callback.getStatus()); // Optional
        }
    }


    private String extractCallbackData(Map<String, String> payload) {
        log.info("---- [Callback] Extracting callback data from payload...");

        if (payload.containsKey("CALLBACKRESPONSE")) {
            String callbackResponse = payload.get("CALLBACKRESPONSE");
            log.info("---- [Callback] CALLBACKRESPONSE found (Razorpay): {}", callbackResponse);
            return callbackResponse;
        } else if (payload.containsKey("CALLBACK")) {
            String callback = payload.get("CALLBACK");
            log.info("---- [Callback] CALLBACK found (Cashfree): {}", callback);
            return callback;
        }  else if (payload.containsKey("txn_method")) {
            log.info("---- [Callback] Detected Runpaisa payout callback format.");
            // Serialize the Map to JSON and return for parsing
            try {
                return new ObjectMapper().writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.error("Error serializing payout callback: {}", e.getMessage());
            }
        }

        log.warn("---- [Callback] No recognized callback data found in payload.");
        return null;
    }

    private RunPaisaCallback convertToCallback(Map<String, String> payload) {
        log.info("---- [Callback] Converting raw payload to RunPaisaCallback object...");

        RunPaisaCallback callback = new RunPaisaCallback();
        callback.setStatus(payload.get("STATUS"));
        callback.setStatusCode(payload.get("STATUS_CODE"));
        callback.setOrderId(payload.get("ORDER_ID"));
        callback.setTxnMode(payload.get("TXN_MODE"));
        callback.setTxnAmount(payload.get("TXN_AMOUNT"));
        callback.setCardCategory(payload.get("CARD_CATEGORY"));
        callback.setTxnDate(payload.get("TXN_DATE"));
        callback.setTxnInfo(payload.get("TXN_INFO"));
        callback.setCustomerName(payload.get("CUSTOMER_NAME"));
        callback.setCustomerEmail(payload.get("CUSTOMER_EMAIL"));
        callback.setCustomerPhone(payload.get("CUSTOMER_PHONE"));
        callback.setBankTxnId(payload.get("BANK_TXNID"));
        callback.setBankCode(payload.get("BANK_CODE"));
        callback.setErrorId(payload.get("ERROR_ID"));
        callback.setErrorDesc(payload.get("ERROR_DESC"));
        callback.setCardNumber(payload.get("CARD_NUMBER"));
        callback.setCardType(payload.get("CARD_TYPE"));
        callback.setUnmappedStatus(payload.get("UNMAPPED_STATUS"));
        callback.setPgPartner(payload.get("PG_PARTNER"));
        callback.setMercUnqRef(payload.get("MERC_UNQ_REF"));

        log.info("---- [Callback] Callback object created: {}", callback);
        return callback;
    }

}
