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
    public CommonResponse<BillRechargeResponseDTO> processRecharge(BillRechargeRequestDTO request) {
        try {
            Optional<SubProducts> subProducts = subProductsRepository.findById(request.getSubProductId());
            WalletType walletType = WalletType.valueOf(subProducts.get().getWalletType());

            BigDecimal availableBalance = commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(request.getAmount()) < 0) {
                return CommonResponse.error(400, "Insufficient Balance");
            }

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // Build form parameters
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("api_token", credentials.getToken());
            formParams.add("userId", String.valueOf(credentials.getUsername()));
            formParams.add("secretKey", credentials.getClientSecret());
            formParams.add("number", request.getNumber());
            formParams.add("provider", String.valueOf(request.getProvider()));
            formParams.add("amount", request.getAmount().toString());
            formParams.add("clientId", request.getClientId());

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

            BbpsProviderList bbpsProviderRecord = optionalProvider.get();

            String operator = bbpsProviderRecord.getOperator();
            // Fetch scheme rate (variable slab example)
            Optional<SchemeRate> schemeRateOpt = schemeRateRepository.findVariableSlabByOperatorAndAmount(operator, amount);
            SchemeRate schemeRate = schemeRateOpt.orElseThrow(() -> new RuntimeException("SchemeRate not found for operator: " + operator));

            // Find scheme using scheme code
            Scheme scheme = schemeRepository.findBySchemeCode(schemeRate.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for schemeCode: " + schemeRate.getSchemeCode()));

            // Extract productId from schemeCombinationHashCode
            String hashCode = scheme.getSchemeCombinationHashCode();
            if (hashCode == null || hashCode.isEmpty()) {
                throw new RuntimeException("Scheme combination hash code is missing for scheme: " + scheme.getSchemeCode());
            }
            String productIdStr = hashCode.split("-")[0]; // First index before first dash
            Long productId = Long.valueOf(productIdStr);

            // Fetch product and get GST
            Products product = productsRepository.findById(Short.valueOf(String.valueOf(productId)))
                    .orElseThrow(() -> new RuntimeException("Product not found for productId: " + productId));
            String gstValue = String.valueOf(product.getSgst());
            String tdsValue = String.valueOf(product.getTds());

            BbpsTranactionRequestDto bbpsTranactionRequestDto = new BbpsTranactionRequestDto(schemeRate, amount, walletType.name(), retailerId, gstValue, tdsValue, "SUCCESS", baseTransactionId, operator, subProducts.get().getSubProductTitle());

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), request.getRetailerId());
            commonMethods.updateWallet(walletAmountTable, retailerId, walletType, request.getAmount(), "Debit", "TXN" + baseTransactionId, "SUCCESS", "DEBIT");
            bbpsRelatedTransactions.processCharges(bbpsTranactionRequestDto, walletAmountTable);

//            } else {
//                schemeRateOpt = schemeRateRepository.findFixedSlabByOperatorAndAmount(operator, amount);
//            }

            ResponseEntity<BillRechargeResponseDTO> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/v3/bill-recharge",
                    HttpMethod.POST,
                    entity,
                    BillRechargeResponseDTO.class
            );

            BillRechargeResponseDTO apiResponse = response.getBody();

            if (apiResponse == null) {
                return CommonResponse.error(500, "Null response from external API");
            }

            apiResponse.setStatus("SUCCESS");
            if (apiResponse.getStatusId() == 1 || "SUCCESS".equalsIgnoreCase(apiResponse.getStatus())) {

                //if (product.getIsCommissions()) {
                    bbpsRelatedTransactions.processCommission(bbpsTranactionRequestDto);
                //}
                return CommonResponse.success(apiResponse, "Transaction Successful");
            } else if (apiResponse.getStatusId() == 2 || "FAILURE".equalsIgnoreCase(apiResponse.getStatus())) {


                //reverseTransactionOnFailure();

                return CommonResponse.success(apiResponse, "Transaction is Fail");
            } else if (apiResponse.getStatusId() == 3 || "PENDING".equalsIgnoreCase(apiResponse.getStatus())) {
                return CommonResponse.success(apiResponse, "Transaction is Pending");
            } else if (apiResponse.getStatusId() == 34 || "ACCEPTED".equalsIgnoreCase(apiResponse.getStatus())) {
                return CommonResponse.success(apiResponse, "Transaction is Accepted");
            } else if (apiResponse.getStatusId() == 24 || "SUCCESSFULLY SUBMITTED".equalsIgnoreCase(apiResponse.getStatus())) {
                return CommonResponse.success(apiResponse, "Transaction is Accepted");
            } else {
                return CommonResponse.error(401, apiResponse.getMessage());
            }

        } catch (Exception ex) {
            return CommonResponse.error(500, "Error: " + ex.getMessage());
        }
    }

}
