package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.response.BillRechargeCheckStatusResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BBPSService;
import com.im.admin.util.BBPSRelatedTransactions;
import com.im.admin.util.CommonMethods;
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
    public CommonResponse<?> processRecharge(BillRechargeRequestDTO request) {
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
            formParams.add("clientId", baseTransactionId);

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
            SchemeRate schemeRate = schemeRateOpt.orElseThrow(() -> new RuntimeException("SchemeRate not found for operator: " + operator));
            log.info("SchemeRate found: {}", schemeRate.getSchemeCode());

            // Find scheme using scheme code
            Scheme scheme = schemeRepository.findBySchemeCode(schemeRate.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for schemeCode: " + schemeRate.getSchemeCode()));

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

            BbpsTranactionRequestDto bbpsTranactionRequestDto = new BbpsTranactionRequestDto(schemeRate, amount, walletType.name(), retailerId, gstValue, tdsValue, "PENDING", baseTransactionId, operator, subProducts.get().getSubProductTitle());

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), request.getRetailerId());
            commonMethods.updateWallet(walletAmountTable, retailerId, walletType, request.getAmount(), "Debit", baseTransactionId, "PENDING", "DEBIT", subProducts.get().getSubProductTitle());
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
                bbpsRelatedTransactions.processCommission(bbpsTranactionRequestDto);
                return CommonResponse.success(billRechargeResponse, "Transaction Successful");

            } else if (billRechargeResponse.getStatusId() == 2 || "FAILURE".equalsIgnoreCase(billRechargeResponse.getStatus())) {
                bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(retailerId, walletType.name(), baseTransactionId);
                return CommonResponse.error(400, "Transaction is Fail");

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
                    requestParams.add("clientId", baseTransactionId);

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

                        bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(retailerId, walletType.name(), baseTransactionId);
                        return CommonResponse.success(null, "Transaction Failed: " + checkStatusResp.getDescription());
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

                    BBPSPendingTransactions pending = new BBPSPendingTransactions();
                    pending.setTransactionId(baseTransactionId);
                    pending.setRetailerId(retailerId);
                    pending.setWalletType(walletType.name());
                    pending.setAmount(request.getAmount());
                    pending.setSchemeRate(schemeRate);
                    pending.setGst(gstValue);
                    pending.setTds(tdsValue);
                    pending.setOperatorName(operator);
                    pending.setSubProductName(subProducts.get().getSubProductTitle());
                    pending.setVendorBaseUrl(credentials.getBaseUrlUat());
                    pending.setToken(credentials.getToken());
                    pending.setUsername(credentials.getUsername());
                    pending.setClientSecret(credentials.getClientSecret());
                    pending.setStatus("PENDING");                // default status
                    bbpsPendingTransactionsRepository.save(pending);

                    log.info("Saved pending transaction: {}", baseTransactionId);

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

}
