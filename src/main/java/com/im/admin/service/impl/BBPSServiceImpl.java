package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.response.BillRechargeCheckStatusResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BBPSService;
import com.im.admin.util.BBPSRelatedTransactions;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TransactionHelperService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BBPSServiceImpl implements BBPSService {

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private BbpsProviderListRepository bbpsProviderListRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private BBPSRelatedTransactions bbpsRelatedTransactions;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private BBPSPendingTransactionsRepository bbpsPendingTransactionsRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionHelperService transactionHelperService;

    @Override
    public CommonResponse<List<ProviderListResponseDTO.ProviderDTO>> getProviderList(ProviderListRequestDTO requestDTO) {

        // Step 1: Get vendor credentials
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(requestDTO.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // Step 2: Prepare form parameters
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("api_token", credentials.getToken());
        formParams.add("userId", credentials.getUsername());
        formParams.add("secretKey", credentials.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formParams, headers);

        try {
            ResponseEntity<ProviderListResponseDTO> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/v3/get-provider-list",
                    HttpMethod.POST,
                    entity,
                    ProviderListResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().getStatus() == 1) {
                return CommonResponse.success(response.getBody().getProviderList(), response.getBody().getMessage());
            } else {
                return CommonResponse.error(500, "Failed to fetch provider list");
            }

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            return CommonResponse.error(500, "API Error: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return CommonResponse.error(500, "Unexpected Error: " + ex.getMessage());
        }
    }

    @Override
    public FetchBillDetailsResponseDTO fetchBillDetails(FetchBillDetailsRequestDTO requestDTO) {

        // Step 1: Get vendor credentials
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(requestDTO.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // Step 2: Prepare form data
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.set("api_token", credentials.getToken());
        formParams.set("provider", String.valueOf(requestDTO.getProvider()));
        formParams.set("number", requestDTO.getBillerNumber());
        formParams.set("userId", credentials.getUsername());
        formParams.set("secretKey", credentials.getClientSecret());
        formParams.set("customerMobileNumber", requestDTO.getCustomerMobileNumber());

        if (requestDTO.getDob() != null && !requestDTO.getDob().isEmpty())
            formParams.set("dob", requestDTO.getDob());

        if (requestDTO.getField_one() != null && !requestDTO.getField_one().isEmpty())
            formParams.set("field_one", requestDTO.getField_one());

        if (requestDTO.getField_two() != null && !requestDTO.getField_two().isEmpty())
            formParams.set("field_two", requestDTO.getField_two());

        if (requestDTO.getField_three() != null && !requestDTO.getField_three().isEmpty())
            formParams.set("field_three", requestDTO.getField_three());

        if (requestDTO.getField_four() != null && !requestDTO.getField_four().isEmpty())
            formParams.set("field_four", requestDTO.getField_four());

        // Step 3: Make the API call
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formParams, headers);

        try {
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/v3/fetch/bill-details",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBodyStr = rawResponse.getBody();
            System.out.println("Raw Vendor Response: " + responseBodyStr);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            FetchBillDetailsResponseDTO response = objectMapper.readValue(responseBodyStr, FetchBillDetailsResponseDTO.class);
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            System.err.println("External API Error Response: " + ex.getResponseBodyAsString());
            log.error("External API Error: {}", ex.getResponseBodyAsString());
            throw new RuntimeException("External API error: " + ex.getResponseBodyAsString());

        } catch (Exception ex) {
            System.err.println("Unknown Error: " + ex.getMessage());
            log.error("Unexpected error", ex);
            throw new RuntimeException("Something went wrong while calling bill details API", ex);
        }
    }


    @Override
    @Transactional
    public CommonResponse<?> processRecharge(BillRechargeRequestDTO request, Double latitude, Double longitude, String ipAddress) {
        log.info("Starting bill recharge process for retailerId: {}, subProductId: {}, amount: {}",
                request.getRetailerId(), request.getSubProductId(), request.getAmount());
        try {
            Optional<SubProducts> subProducts = subProductsRepository.findById(request.getSubProductId());
            if (subProducts.isEmpty()) {
                log.error("SubProduct not found for ID: {}", request.getSubProductId());
                return CommonResponse.error(404, "SubProduct not found");
            }
            WalletType walletType = WalletType.valueOf(subProducts.get().getWalletType());
            BigDecimal availableBalance = commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());
            log.info("Available balance for retailerId {} in wallet {}: {}", request.getRetailerId(), walletType.name(), availableBalance);

            if (availableBalance.compareTo(request.getAmount()) < 0) {
                log.warn("Insufficient balance. Required: {}, Available: {}", request.getAmount(), availableBalance);
                return CommonResponse.error(400, "Insufficient Balance");
            }

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated baseTransactionId: {}", baseTransactionId);

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);
            log.info("Vendor selected: {}", vendor.getVendorName());

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            log.debug("Vendor credentials retrieved for vendorId: {}", vendor.getId());

            // Build form parameters
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("api_token", credentials.getToken());
            formParams.add("userId", String.valueOf(credentials.getUsername()));
            formParams.add("secretKey", credentials.getClientSecret());
            formParams.add("number", request.getNumber());
            formParams.add("provider", String.valueOf(request.getProvider()));
            formParams.add("amount", request.getAmount().toString());
            formParams.add("clientId", "TXN"+baseTransactionId);

            // Optional fields
            if (request.getContext() != null) formParams.add("context", request.getContext());
            if (request.getBill_due_date() != null) formParams.add("bill_due_date", request.getBill_due_date());
            if (request.getConsumerNumber() != null) formParams.add("consumerNumber", request.getConsumerNumber());
            if (request.getDob() != null) formParams.add("dob", request.getDob());
            if (request.getField_one() != null) formParams.add("field_one", request.getField_one());
            if (request.getField_two() != null) formParams.add("field_two", request.getField_two());
            if (request.getField_three() != null) formParams.add("field_three", request.getField_three());
            if (request.getField_four() != null) formParams.add("field_four", request.getField_four());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formParams, headers);

            String retailerId = request.getRetailerId();
            Double amount = request.getAmount().doubleValue();

            Optional<BbpsProviderList> optionalProvider = bbpsProviderListRepository.findByVendorId(request.getProvider());
            if (optionalProvider.isEmpty()) {
                log.error("BBPS Provider not found for vendorId: {}", request.getProvider());
                return CommonResponse.error(404, "BBPS Provider not found");
            }

            BbpsProviderList bbpsProviderRecord = optionalProvider.get();
            String operator = bbpsProviderRecord.getOperator();
            log.info("Operator identified: {}", operator);

            // Fetch scheme rate (variable slab example)
            Optional<SchemeRate> schemeRateOpt = schemeRateRepository.findVariableSlabByOperatorAndAmount(operator, amount);
            SchemeRate schemeRate = schemeRateOpt.orElseThrow(() -> new RuntimeException("Slab not found for operator: " + operator));
            log.info("SchemeRate found: {}", schemeRate.getSchemeCode());

            // Find scheme using scheme code
            Scheme scheme = schemeRepository.findBySchemeCode(schemeRate.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for schemeCode: " + schemeRate.getSchemeCode()));

            if (!Boolean.TRUE.equals(scheme.getIsActive())) {
                log.warn("Scheme is inactive for schemeCode: {}", scheme.getSchemeCode());
                return CommonResponse.error(400, "Scheme is currently inactive");
            }

            // Extract productId from schemeCombinationHashCode
            String hashCode = scheme.getSchemeCombinationHashCode();
            if (hashCode == null || hashCode.isEmpty()) {
                log.error("Scheme combination hash code missing for schemeCode: {}", scheme.getSchemeCode());
                throw new RuntimeException("Scheme combination hash code is missing for scheme: " + scheme.getSchemeCode());
            }
            String productIdStr = hashCode.split("-")[0];
            Long productId = Long.valueOf(productIdStr);

            // Fetch product and get GST
            Products product = productsRepository.findById(Short.valueOf(String.valueOf(productId)))
                    .orElseThrow(() -> new RuntimeException("Product not found for productId: " + productId));
            String gstValue = String.valueOf(product.getSgst());
            String tdsValue = String.valueOf(product.getTds());

            BbpsTranactionRequestDto bbpsTranactionRequestDto = new BbpsTranactionRequestDto(schemeRate, amount, walletType.name(), retailerId, gstValue, tdsValue, "PENDING", baseTransactionId, operator, subProducts.get().getSubProductTitle(), latitude, longitude, "A2Z", "Not Yet");

            RetailersExtraColumnsDto retailerExtraColumns =
                    RetailersExtraColumnsDto.builder()
                            .medium(request.getMedium())
                            .clientIp(ipAddress)
                            .requestDomainName(request.getRequestDomainName())
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), request.getRetailerId());
            commonMethods.updateWallet(walletAmountTable, retailerId, walletType, request.getAmount(), "Debit", baseTransactionId, "PENDING", "DEBIT", subProducts.get().getSubProductTitle(),
                    "Tx Amount for A2Z Billpay For " + operator +"#"+ bbpsProviderRecord.getSubOperator() ,
                    "A2Z Billpay Transaction from " + walletType.name(),schemeRate.getSchemeCode(), retailerExtraColumns, vendor.getVendorName(), schemeRate.getOperatorName());
            bbpsRelatedTransactions.processCharges(bbpsTranactionRequestDto, walletAmountTable);

            log.info("Wallet updated and charges processed for transactionId: {}", baseTransactionId);

            ResponseEntity<BillRechargeResponseDTO> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/v3/bill-recharge",
                    HttpMethod.POST,
                    entity,
                    BillRechargeResponseDTO.class
            );

            BillRechargeResponseDTO billRechargeResponse = response.getBody();

            if (billRechargeResponse == null) {
                return CommonResponse.error(500, "No response from external API");
            }

            //BillRechargeResponseDTO apiResponse = new BillRechargeResponseDTO();
            billRechargeResponse.setStatus(billRechargeResponse.getStatus());
            billRechargeResponse.setStatusId(billRechargeResponse.getStatusId());

            BillRechargeCheckStatusResponseDto checkStatusResp = null;

            if (billRechargeResponse.getStatusId() == 1 || "SUCCESS".equalsIgnoreCase(billRechargeResponse.getStatus())) {
                bbpsRelatedTransactions.updateTransactionStatus(retailerId, walletType.name(), baseTransactionId);

                mirrorTransactionToAdminOrWhitelabel(retailerId, request.getAmount(),
                        walletType.name(), "TXN" + baseTransactionId, bbpsTranactionRequestDto);

                bbpsRelatedTransactions.processCommission(bbpsTranactionRequestDto);

                return CommonResponse.success(billRechargeResponse, "Transaction Successful");

            } else if (billRechargeResponse.getStatusId() == 2 || "FAILURE".equalsIgnoreCase(billRechargeResponse.getStatus())) {

                savePendingTransaction(
                        baseTransactionId,
                        retailerId,
                        walletType,
                        request,
                        schemeRate,
                        gstValue,
                        tdsValue,
                        operator,
                        subProducts.get(),
                        credentials,
                        billRechargeResponse.getStatus(), // dynamic status
                        billRechargeResponse.getMessage()
                );

                bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(retailerId, walletType.name(), baseTransactionId);
                return CommonResponse.failedResponse(400, "Transaction is Fail", billRechargeResponse);

            } else if (
                    (billRechargeResponse.getStatusId() == 3 || "PENDING".equalsIgnoreCase(billRechargeResponse.getStatus())) ||
                            (billRechargeResponse.getStatusId() == 34 || "ACCEPTED".equalsIgnoreCase(billRechargeResponse.getStatus())) ||
                            (billRechargeResponse.getStatusId() == 24 || "SUCCESSFULLY SUBMITTED".equalsIgnoreCase(billRechargeResponse.getStatus()) ||
                                    (billRechargeResponse.getStatusId() == 59 || "INPROCESS".equalsIgnoreCase(billRechargeResponse.getStatus())) ||
                                    (billRechargeResponse.getStatusId() == 33 || "PROCESSING".equalsIgnoreCase(billRechargeResponse.getStatus())) ||
                                    (billRechargeResponse.getStatusId() == 90 || "INITIATED".equalsIgnoreCase(billRechargeResponse.getStatus())))
            ) {
                int maxRetries = 3;
                boolean successFound = false;


                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    log.info("Pending detected. Attempt {}/{} to check vendor status...", attempt, maxRetries);

                    // Build form parameters
                    MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
                    requestParams.add("api_token", credentials.getToken());
                    requestParams.add("userId", String.valueOf(credentials.getUsername()));
                    requestParams.add("secretKey", credentials.getClientSecret());
                    requestParams.add("clientId", "TXN"+baseTransactionId);

                    HttpHeaders header = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                    HttpEntity<MultiValueMap<String, String>> checkStatusRequest = new HttpEntity<>(requestParams, header);

                    ResponseEntity<BillRechargeCheckStatusResponseDto> checkStatusResponse = restTemplate.exchange(
                            credentials.getBaseUrlUat() + "/v3/check-status",
                            HttpMethod.POST,
                            checkStatusRequest,
                            BillRechargeCheckStatusResponseDto.class
                    );

                    checkStatusResp = checkStatusResponse.getBody();

                    if (checkStatusResp == null) {
                        return CommonResponse.error(500, "Null response from external API");
                    }

                    if ("SUCCESS".equalsIgnoreCase(checkStatusResp.getMessage()) ||
                            checkStatusResp.getStatus() != null && checkStatusResp.getStatus() == 43) {

                        log.info("Vendor reported SUCCESS on attempt {}", attempt);

                        bbpsRelatedTransactions.updateTransactionStatus(retailerId, walletType.name(), baseTransactionId);
                        bbpsRelatedTransactions.processCommission(bbpsTranactionRequestDto);

                        successFound = true;
                        break;
                    } else if ("FAILED".equalsIgnoreCase(checkStatusResp.getMessage()) ||
                            (checkStatusResp.getStatus() != null && checkStatusResp.getStatus() == 55)) {

                        savePendingTransaction(
                                baseTransactionId,
                                retailerId,
                                walletType,
                                request,
                                schemeRate,
                                gstValue,
                                tdsValue,
                                operator,
                                subProducts.get(),
                                credentials,
                                "FAILED", // dynamic status
                                checkStatusResp.getMessage()
                        );

                        bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(retailerId, walletType.name(), baseTransactionId);
                        return CommonResponse.failedResponse(400, "Transaction Failed", checkStatusResponse);
                    }

                    // Wait before retrying
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (!successFound) {
                    log.info("Transaction still pending after {} attempts. Leaving for cron job to handle.", maxRetries);

                    savePendingTransaction(
                            baseTransactionId,
                            retailerId,
                            walletType,
                            request,
                            schemeRate,
                            gstValue,
                            tdsValue,
                            operator,
                            subProducts.get(),
                            credentials,
                            "PENDING", // dynamic status
                            checkStatusResp.getMessage()
                    );

                    return CommonResponse.success(checkStatusResp, "Transaction still pending. Will complete shortly.");
                }

                return CommonResponse.success(checkStatusResp, "Transaction is Successful");

            } else {
                return CommonResponse.error(401, checkStatusResp.getMessage());
            }

        } catch (Exception ex) {
            return CommonResponse.error(500, "Error: " + ex.getMessage());
        }
    }

    private void savePendingTransaction(
            String baseTransactionId,
            String retailerId,
            WalletType walletType,
            BillRechargeRequestDTO request,
            SchemeRate schemeRate,
            String gstValue,
            String tdsValue,
            String operator,
            SubProducts subProduct,
            VendorCredentials credentials,
            String status,
            String actualVendorStatus
    ) {
        try {
            BBPSPendingTransactions pending = new BBPSPendingTransactions();
            pending.setTransactionId("TXN" + baseTransactionId);
            pending.setRetailerId(retailerId);
            pending.setWalletType(walletType.name());
            pending.setAmount(request.getAmount());
            pending.setSchemeRate(schemeRate);
            pending.setGst(gstValue);
            pending.setTds(tdsValue);
            pending.setOperatorName(operator);
            pending.setSubProductName(subProduct.getSubProductTitle());
            pending.setVendorBaseUrl(credentials.getBaseUrlUat());
            pending.setToken(credentials.getToken());
            pending.setUsername(credentials.getUsername());
            pending.setClientSecret(credentials.getClientSecret());
            pending.setStatus(status); // dynamic status
            pending.setActualVendorStatus(actualVendorStatus);
            pending.setConsumerNumber(request.getNumber());

            bbpsPendingTransactionsRepository.save(pending);
            log.info("Saved pending transaction with status [{}]: {}", status, baseTransactionId);

        } catch (Exception e) {
            log.error("Failed to save pending transaction [{}]: {}", baseTransactionId, e.getMessage(), e);
            throw new RuntimeException("Error saving pending transaction: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommonResponse<FetchReceiptResponseDto> fetchReceipt(String txnId) {
        try {
            BBPSPendingTransactions transaction = bbpsPendingTransactionsRepository.findByTransactionId(txnId);

            if (transaction == null) {
                return CommonResponse.failure("Transaction not found");
            }

            FetchReceiptResponseDto dto = getFetchReceiptResponseDto(transaction);
            return CommonResponse.success(dto, "Transaction fetched successfully");

        } catch (Exception e) {
            log.error("Error fetching transaction receipt", e);
            throw new RuntimeException("Failed to fetch receipt", e);
        }
    }

    private static FetchReceiptResponseDto getFetchReceiptResponseDto(BBPSPendingTransactions transaction) {
        FetchReceiptResponseDto dto = new FetchReceiptResponseDto();
        dto.setRetailerId(transaction.getRetailerId());
        dto.setCreatedDateTime(transaction.getCreatedAt());
        dto.setTxnId(transaction.getTransactionId());
        dto.setTxnStatus(transaction.getActualVendorStatus());
        dto.setTxnAmount(transaction.getAmount());
        dto.setConsumerNumber(transaction.getConsumerNumber());
        dto.setRefNo(null); // Or any other reference you want
        return dto;
    }

    private void mirrorTransactionToAdminOrWhitelabel(String hierarchyId, BigDecimal amount, String walletType,
                                                      String txnId, BbpsTranactionRequestDto request) {

        log.info("=== [mirrorTransactionToAdminOrWhitelabel] START ===");
        log.info("Starting mirror chain from HierarchyID: {} | TxnType: {} | Amount: {} | WalletType: {} | TxnId: {}",
                hierarchyId, "debit", amount, walletType, txnId);

        String currentId = hierarchyId;
        boolean mirrorDone = false;

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("No parent found for hierarchy ID: {} — stopping mirror chain", currentId);
                break;
            }

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            log.info("Found Parent | Current: {} | Parent: {} | ParentType: {}", currentId, parentId, parentType);

            if ("WL".equalsIgnoreCase(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    log.info("Detected WhiteLabel Admin [{}]. Proceeding with mirror transaction.", parentId);
                    mirrorTransactionToHierarchy(parentId, hierarchyId, "debit", amount, walletType, txnId, "Credit", "SUCCESS", request);
                    mirrorDone = true;
                    break;
                } else {
                    log.debug("Parent [{}] is WhiteLabel but not Admin — continuing search.", parentId);
                }
            }

            if ("IM".equalsIgnoreCase(parentType) || "AD".equalsIgnoreCase(parentType)) {
                log.info("Detected Admin Hierarchy [{} - {}]. Proceeding with mirror transaction.", parentId, parentType);
                mirrorTransactionToHierarchy(parentId, hierarchyId, "debit", amount, walletType, txnId, "Credit", "SUCCESS", request);
                mirrorDone = true;
                break;
            }

            currentId = parentId;
        }

        if (!mirrorDone) {
            log.warn("Mirror transaction not executed for hierarchy [{}]. No eligible parent found.", hierarchyId);
        }

        log.info("=== [mirrorTransactionToAdminOrWhitelabel] END ===");
    }

    @Transactional
    private void mirrorTransactionToHierarchy(String targetHierarchyId, String sourceHierarchyId, String originalTxnType,
                                              BigDecimal amount, String walletType, String txnId, String action, String tranStatus,
                                              BbpsTranactionRequestDto request) {

        log.info("=== [mirrorTransactionToHierarchy] START ===");
        log.info("Mirroring Transaction | From: {} | To: {} | OriginalTxnType: {} | WalletType: {} | Amount: {} | TxnId: {} | Status: {}",
                sourceHierarchyId, targetHierarchyId, originalTxnType, walletType, amount, txnId, tranStatus);

        String targetType = commonMethods.extractHierarchyType(targetHierarchyId);
        String mirrorTxnType = originalTxnType.equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";

        log.debug("Derived Mirror Transaction Type: {} for TargetType: {}", mirrorTxnType, targetType);

        BigDecimal currentBalance = commonMethods.getCurrentBalance(targetHierarchyId, walletType);
        log.debug("Current Balance for Target [{} - {}]: {}", targetHierarchyId, walletType, currentBalance);

        if ("DEBIT".equalsIgnoreCase(mirrorTxnType) && currentBalance.compareTo(amount) < 0) {
            log.error("{} ({}) has insufficient balance to mirror DEBIT transaction. Required: {}, Available: {}",
                    targetType, targetHierarchyId, amount, currentBalance);
            throw new RuntimeException(targetType + " wallet has insufficient balance to mirror transaction.");
        }

        BigDecimal afterBalance = mirrorTxnType.equalsIgnoreCase("CREDIT")
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        String actionOnBalanceAmount = mirrorTxnType.equalsIgnoreCase("DEBIT")
                ? "Debit Funds " + action + " - " + walletType.toUpperCase()
                : "Add Funds " + action + " - " + walletType.toUpperCase();

        log.info("Mirror Transaction Prepared | TargetType: {} | MirrorTxnType: {} | BeforeBalance: {} | AfterBalance: {} | Action: {}",
                targetType, mirrorTxnType, currentBalance, afterBalance, actionOnBalanceAmount);

        try {
            insertTransactionForAdminHierarchy(
                    commonMethods.getTransactionTable(targetType, walletType),
                    mirrorTxnType,
                    amount,
                    "Mirror Transaction for " + originalTxnType,
                    sourceHierarchyId,
                    walletType,
                    currentBalance,
                    afterBalance,
                    actionOnBalanceAmount,
                    txnId, tranStatus, request
            );
            log.info("Mirror transaction inserted successfully for target [{} - {}]", targetHierarchyId, targetType);
        } catch (Exception e) {
            log.error("Failed to insert mirror transaction for [{} - {}]: {}", targetHierarchyId, targetType, e.getMessage(), e);
            throw e;
        }

        try {
            updateAmountTable(targetType, targetHierarchyId, commonMethods.getAmountTable(targetType, walletType), currentBalance.subtract(afterBalance));
            log.info("Updated amount table successfully for target [{} - {}]", targetHierarchyId, targetType);
        } catch (Exception e) {
            log.error("Failed to update amount table for [{} - {}]: {}", targetHierarchyId, targetType, e.getMessage(), e);
            throw e;
        }

        log.info("=== [mirrorTransactionToHierarchy] END ===");
    }

    @Transactional
    private void insertTransactionForAdminHierarchy(
            String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId,
            String walletType, BigDecimal beforeBalance, BigDecimal afterBalance,
            String actionOnBalanceAmount, String tranxId, String tranStatus, BbpsTranactionRequestDto request) {

        log.info("[ADMIN_TXN_INSERT] Starting insert for Admin Hierarchy | Table={} | HierarchyID={} | Amount={} | WalletType={} | TranxID={}",
                transactionTable, hierarchyId, amount, walletType, tranxId);

        String operatorName = request.getOperatorName();
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        String sql = "INSERT INTO " + transactionTable +
                " (hierarchy_id, service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, operator_name, latitude, longitude) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, :operatorName, :latitude, :longitude)";

        log.debug("[ADMIN_TXN_INSERT] Generated SQL: {}", sql);

        try {
            entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("remarks", remarks)
                    .setParameter("dateOfTransaction", java.sql.Date.valueOf(LocalDate.now()))
                    .setParameter("timeOfTransaction", java.sql.Time.valueOf(LocalTime.now()))
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("operatorName", operatorName)
                    .setParameter("latitude", latitude)
                    .setParameter("longitude", longitude)
                    .executeUpdate();

            log.info("[ADMIN_TXN_INSERT] Successfully inserted transaction for Admin | HierarchyID={} | TranxID={} | Amount={} | Status={}",
                    hierarchyId, tranxId, amount, tranStatus);

        } catch (Exception e) {
            log.error("[ADMIN_TXN_INSERT] Failed to insert transaction for Admin | HierarchyID={} | TranxID={} | Error={}",
                    hierarchyId, tranxId, e.getMessage(), e);
            throw e;
        }
    }

    public void updateAmountTable(String hierarchyType, String hierarchyId, String amountTable, BigDecimal amount) {
        log.info("Updating {} | Hierarchy Type: {} | Hierarchy ID: {} | Final Amount to Add: {}",
                amountTable, hierarchyType, hierarchyId, amount);

        String columnName = transactionHelperService.getHierarchyColumnName(hierarchyType);

        String sql = "UPDATE " + amountTable + " SET " +
                "available_balance = available_balance + :amount, " +
                "last_updated_amount = :amount, " +
                " last_updated_date = :date, " +
                " last_updated_time = :time " +
                "WHERE " + columnName + " = :hierarchyId";

        int updated = entityManager.createNativeQuery(sql)
                .setParameter("amount", amount)
                .setParameter("date", new Date())
                .setParameter("time", new Time(System.currentTimeMillis()))
                .setParameter("hierarchyId", hierarchyId)
                .executeUpdate();

        entityManager.flush();

        if (updated == 0) {
            throw new RuntimeException("Wallet update failed for: " + hierarchyId);
        }

    }

}
