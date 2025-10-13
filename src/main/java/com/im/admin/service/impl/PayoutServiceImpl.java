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

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

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
            String txnId = "TXN" + baseTransactionId;

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
                payoutJsonRequestBody.setOrderId(txnId);
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
                commonMethods.updateWallet(walletAmountTable, sendPayoutRequest.getRetailerId(), walletType, sendPayoutRequest.getAmount(), "Debit", "TXN" + baseTransactionId, "VENDOR_PENDING", "DEBIT", subProducts.get().getSubProductTitle());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", generateTokenForPayout(credentials));

                Map<String, Object> payout = new LinkedHashMap<>();
                payout.put("account_number", String.valueOf(sendPayoutRequest.getAccountNumber()));  // "026291800001191"
                payout.put("ifsc_code", String.valueOf(sendPayoutRequest.getIfscCode()));            // "YESB0000001"
                payout.put("amount", String.format("%.2f", sendPayoutRequest.getAmount()));          // "11.00"
                payout.put("order_id", txnId);                                   // "TXN25082012270100272"
                payout.put("beneficiary_name", String.valueOf(sendPayoutRequest.getBeneficiaryName()));
                payout.put("payment_mode", String.valueOf(sendPayoutRequest.getPaymentMode()));      // "IMPS"

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payout, headers);

                String url = credentials.getBaseUrlProd() + "/payment";
                log.info("Sending payout request to Run Paisa. URL: {}, Headers: {}, Body: {}", url, headers, payout);

                ResponseEntity<PayoutResponseDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        PayoutResponseDto.class
                );

                PayoutResponseDto payoutResponse = response.getBody();
                assert payoutResponse != null;

                String vendorStatus = payoutResponse.getStatus() != null ? payoutResponse.getStatus().toUpperCase() : "UNKNOWN";

                log.info("Run Paisa payout response received for TXN_ID={}, STATUS={}, MESSAGE={}",
                        txnId, vendorStatus, payoutResponse.getMessage());

                // Always save transaction irrespective of status
                PayoutPendingTransactions pending = new PayoutPendingTransactions();
                pending.setTransactionId(txnId);
                pending.setSchemeCode(sendPayoutRequest.getSchemeCode());
                pending.setRetailerId(sendPayoutRequest.getRetailerId());
                pending.setAmount(sendPayoutRequest.getAmount());
                pending.setTransactionType(sendPayoutRequest.getPaymentMode());
                pending.setStatus(vendorStatus); // store vendor response status
                pending.setCreatedAt(LocalDateTime.now());

                payoutPendingTransactionRepository.save(pending);

                log.info("Payout transaction saved in DB: [TxnID={}, RetailerID={}, Amount={}, Status={}]",
                        txnId, sendPayoutRequest.getRetailerId(), sendPayoutRequest.getAmount(), vendorStatus);

                // Return appropriate response to frontend
                return switch (vendorStatus) {
                    case "SUCCESS" -> new CommonResponse<>(200, payoutResponse.getMessage(), payoutResponse);
                    case "FAILED" -> new CommonResponse<>(400, payoutResponse.getMessage(), payoutResponse);
                    case "PENDING", "ACCEPTED" ->
                            new CommonResponse<>(200, "Transaction accepted, pending confirmation", payoutResponse);
                    default -> new CommonResponse<>(400, "Unknown vendor response", payoutResponse);
                };
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
        log.info("Initiating callback URL update for subProductId={}", request.getSubProductId());

        VendorRouting routing = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
        log.debug("VendorRouting retrieved: {}", routing);

        Vendors vendor = CommonMethods.getVendors(routing);
        log.debug("Vendor resolved: id={}, productId={}", vendor.getId(), vendor.getProduct().getId());

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("No credentials found for vendorId={}, productId={}", vendor.getId(), vendor.getProduct().getId());
                    return new RuntimeException("No credentials found for selected vendor");
                });

        log.debug("VendorCredentials retrieved: {}", credentials);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", generateTokenForPayout(credentials));

        log.debug("Successfully Generated token for payout");

        Map<String, Object> body = new HashMap<>();
        body.put("callback_url", credentials.getCallbackUrl());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        log.info("Sending request to update callback URL: url={}, headers={}, body={}", credentials.getBaseUrlProd(), headers, body);

        String url = credentials.getBaseUrlProd();

        ResponseEntity<PayoutMerchantCallbackResponse> response = restTemplate.exchange(
                url + "/updatemerchantcallback",
                HttpMethod.POST,
                requestEntity,
                PayoutMerchantCallbackResponse.class
        );

        log.info("Received response: statusCode={}, body={}", response.getStatusCode(), response.getBody());
        return response.getBody();
    }

    //{code=RP001, status=FAILED, message=Transaction Failed, order_id=TXN25101118122900973, utr_no=null, txn_amount=1}
    @Override
    public String handlePayoutCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {

        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("---- [PAYOUT CALLBACK] Payload received: {}", payload);

        // Convert directly to your DTO
        RunpaisaPayoutCallback payoutCallback = mapper.convertValue(payload, RunpaisaPayoutCallback.class);
        log.info("---- [PAYOUT CALLBACK] Parsed DTO: {}", payoutCallback);

        // Create a unified callback object
        RunPaisaCallback callback = new RunPaisaCallback();
        callback.setOrderId(payoutCallback.getOrder_id());
        callback.setTxnAmount(payoutCallback.getTxn_amount());
        callback.setStatus(payoutCallback.getStatus());
        callback.setStatusCode(payoutCallback.getCode());
        callback.setTxnInfo(payoutCallback.getMessage());
        callback.setBankTxnId(payoutCallback.getUtr_no());

        log.info("<------------------ Calling Process Callback Method ------------------>");
        processCallback(callback);

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

    //{code=RP001, status=FAILED, message=Transaction Failed, order_id=TXN25101118122900973, utr_no=null, txn_amount=1}
    //@Override
    public void processCallback(RunPaisaCallback callback) {
        log.info("========== [RunPaisa CALLBACK RECEIVED] ==========");
        log.info("ORDER_ID: {}, STATUS: {}, TXN_AMOUNT: {}, CUSTOMER_PHONE: {}",
                callback.getOrderId(), callback.getStatus(), callback.getTxnAmount(), callback.getCustomerPhone());

        TransactionSchemeDetailsRequestDto dto = null;

        try {
            // Fetch pending transaction (for all statuses)
            log.info("[1] Fetching pending transaction for ORDER_ID: {}", callback.getOrderId());
            PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                    .findByTransactionIdAndStatus(callback.getOrderId(), callback.getStatus())
                    .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + callback.getOrderId()));

            log.info("[1.1] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                    pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                    pendingTxn.getAmount(), pendingTxn.getStatus());

            Double txnAmt = Double.valueOf(callback.getTxnAmount());

            // Fetch matching SchemeRate
            log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                    pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            pendingTxn.getSchemeCode(),
                            pendingTxn.getTransactionType(),
                            txnAmt)
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

            log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                    selectedScheme.getGross(), selectedScheme.getRetailer());

            // Fetch scheme entity
            log.info("[3] Fetching Scheme entity for SchemeCode={}", selectedScheme.getSchemeCode());
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

            log.info("[3.1] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

            // Parse scheme combination hash code
            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            if (hashParts.length < 2) {
                throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.getSchemeCombinationHashCode());
            }

            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];

            log.info("[4] Extracted ProductID={}, SubProductID={} from SchemeCombinationHashCode", productId, subProductIdStr);

            // Fetch product and subproduct
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                    .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

            log.info("[4.1] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                    product.getName(), subProduct.getSubProductTitle());

            // Prepare DTO (common for all statuses)
            log.info("[5] Constructing TransactionSchemeDetailsRequestDto...");
            dto = new TransactionSchemeDetailsRequestDto();
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

            // Get hierarchy ID
            log.info("[6] Fetching user by mobile number: {}", callback.getCustomerPhone());
            String hierarchyId = userDetailsRepository.findByMobileNumber(callback.getCustomerPhone())
                    .orElseThrow(() -> new RuntimeException("No User Found with mobile: " + callback.getCustomerPhone()))
                    .getHierarchyId();
            dto.setHierarchyId(hierarchyId);

            log.info("[6.1] Hierarchy ID fetched successfully: {}", hierarchyId);
            log.info("[7] DTO ready for transaction processing: {}", dto);

            // Process transaction for any status (SUCCESS, FAILED, PENDING, etc.)
            transactionServiceImpl.processTransaction(dto, callback.getStatus());
            log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                    callback.getOrderId(), callback.getStatus());

        } catch (Exception ex) {
            log.error("[ERROR] Exception while processing callback for ORDER_ID={} | Error={}",
                    callback.getOrderId(), ex.getMessage(), ex);
        }

        log.info("========== [RunPaisa CALLBACK PROCESSING COMPLETED] ORDER_ID={} ==========", callback.getOrderId());
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
        } else if (payload.containsKey("txn_method")) {
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
