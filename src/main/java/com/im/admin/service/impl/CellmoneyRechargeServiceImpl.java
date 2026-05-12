package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.CellmobeyRechargeCallback;
import com.im.admin.request.MobileLookupRequest;
import com.im.admin.request.RechargePlanRequest;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.*;
import com.im.admin.service.RechargeCallbackHandler;
import com.im.admin.service.RechargeVendorService;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service("Cellmoney Recharge")
public class CellmoneyRechargeServiceImpl implements RechargeVendorService, RechargeCallbackHandler {


    @Autowired
    private SchemeRepository schemeRepository;

    private static final String SMS_PIN = "5518";

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getVendorCode() {
        return "Cellmoney Recharge";
    }


    @Override
    public RechargeResponse recharge(RechargeRequest request,
                                     String orderId,
                                     VendorCredentials credentials) {

        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // Build Message
        String message = request.getOperatorCode() + "$"
                + request.getCustomerNumber() + "$"
                + request.getAmount() + "$"
                + SMS_PIN + "$"
                + request.getRechargeType() + "$"
                + orderId;

        // Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        String transactionType =
                request.getOperatorName() + "(" + request.getOperatorCode() + ")";

        // Save transaction as PENDING
        RechargeTransaction transaction = RechargeTransaction.builder()
                .schemeCode(request.getSchemeCode())
                .transactionId(orderId)
                .transactionRef(orderId)
                .operatorCode(request.getOperatorCode())
                .customerNumber(request.getCustomerNumber())
                .amount(request.getAmount())
                .rechargeType(request.getRechargeType())
                .transactionType(transactionType)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .retailerId(retailer.getRetailerId())
                .vendor(credentials.getVendor())
                .build();

        rechargeTransactionRepository.save(transaction);

        // Call Vendor API
        String vendorResponse = restTemplate.getForObject(url, String.class);
        transaction.setVendorResponse(vendorResponse);

        String[] parts = vendorResponse.split("=");
        int statusCode = Integer.parseInt(parts[0]);
        String statusMessage = parts.length > 1 ? parts[1] : "Unknown Response";

        //  Vendor rejected
        if (statusCode != 0) {

            transaction.setStatus("FAILED");
            rechargeTransactionRepository.save(transaction);

            return RechargeResponse.builder()
                    .transactionRef(orderId)
                    .status("FAILED")
                    .message(statusMessage)
                    .build();
        }

        // Vendor accepted → PENDING
        transaction.setStatus("PENDING");
        rechargeTransactionRepository.save(transaction);

        //  Immediate status check
        try {

            String statusResponse = this.checkStatusByTxnRef(orderId, credentials);
            log.info("[IMMEDIATE_STATUS] Response={}", statusResponse);

            int checkStatusCode = parseStatusCode(statusResponse);
            String finalStatus = mapStatus(checkStatusCode);

            if (!"PENDING".equalsIgnoreCase(finalStatus)) {

                transaction.setStatus(finalStatus);
                transaction.setVendorResponse(statusResponse);
                rechargeTransactionRepository.save(transaction);


                SchemeRate schemeRate = schemeRateRepository
                        .findSchemeRateByCodeAndOperatorAndAmount(
                                transaction.getSchemeCode(),
                                transaction.getTransactionType(),
                                transaction.getAmount().doubleValue())
                        .orElseThrow(() -> new RuntimeException("SchemeRate not found"));

                Scheme scheme = schemeRepository
                        .findBySchemeCode(schemeRate.getSchemeCode())
                        .orElseThrow(() -> new RuntimeException("Scheme not found"));

                String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

                if (hashParts.length < 2) {
                    throw new RuntimeException("Invalid scheme hash");
                }

                short productId = Short.parseShort(hashParts[0]);
                String subProductId = hashParts[1];

                Products product = productsRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                SubProducts subProduct = subProductsRepository.findById(subProductId)
                        .orElseThrow(() -> new RuntimeException("SubProduct not found"));


                //  ENGINE CALL
                TransactionSchemeDetailsRequestDto dto =
                        buildTransactionDTO(transaction, schemeRate, scheme, product, subProduct);

                transactionEngineServiceImpl.processTransaction(dto, finalStatus);

                return RechargeResponse.builder()
                        .transactionRef(orderId)
                        .status(finalStatus)
                        .message("Processed instantly")
                        .build();
            }

        } catch (Exception e) {
            log.error("[IMMEDIATE_STATUS] Error {}", e.getMessage(), e);
        }

        // Default response
        return RechargeResponse.builder()
                .transactionRef(orderId)
                .status("PENDING")
                .message(statusMessage)
                .build();
    }

    @Override
    public String checkBalance(RechargeRequest request, VendorCredentials credentials) {


        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // 🔹 Build Message
        String message = "BAL$" + SMS_PIN;

        // 🔹 Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        // 🔹 Call Vendor API
        String vendorResponse =
                restTemplate.getForObject(url, String.class);

        return vendorResponse;
    }

    @Override
    public String checkStatus(RechargeRequest request, VendorCredentials credentials) {

        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));


        // Build Message
        String message = "STATUS$"
                + request.getTransactionRef() + "$"
                + SMS_PIN;

        // Build URL
        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        //Call Vendor API
        String vendorResponse =
                restTemplate.getForObject(url, String.class);

        return vendorResponse;
    }

    @Override
    public String checkStatusByTxnRef(String transactionRef,
                                      VendorCredentials credentials) {

        String message = "STATUS$"
                + transactionRef + "$"
                + SMS_PIN;

        String url = UriComponentsBuilder
                .fromHttpUrl(credentials.getBaseUrlUat())
                .queryParam("UserID", credentials.getUsername())
                .queryParam("Password", credentials.getPassword())
                .queryParam("MobileNo", credentials.getUsername())
                .queryParam("Message", message)
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {


        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("========== [CELLMOBEY RECHARGE CALLBACK RECEIVED] ==========");
        log.info("Payload: {}", payload);

        try {

            // Convert payload to DTO
            CellmobeyRechargeCallback rechargeCallback =
                    objectMapper.convertValue(payload, CellmobeyRechargeCallback.class);

            log.info("Parsed Recharge DTO: {}", rechargeCallback);

            // Create unified callback object
            RechargeCallbackData callback = new RechargeCallbackData();

            callback.setApiCode(rechargeCallback.getApiCode());
            callback.setTransactionRef(rechargeCallback.getTransactionRef());
            callback.setStatus(rechargeCallback.getStatus());
            callback.setStatusMessage(rechargeCallback.getStatusMessage());
            callback.setVendorTxnId(rechargeCallback.getVendorTxnId());
            callback.setOperatorTxnId(rechargeCallback.getOperatorTxnId());
            callback.setPreBalance(rechargeCallback.getPreBalance());
            callback.setPostBalance(rechargeCallback.getPostBalance());
            callback.setDiscountPercent(rechargeCallback.getDiscountPercent());
            callback.setDiscountRs(rechargeCallback.getDiscountRs());

            log.info("<------------- Calling processCallback() ------------->");

            processCallback(callback);

            commonFunctions.saveCallbackEventLog(
                    payload,
                    requestTime,
                    clientIp,
                    "Recharge callback processed successfully",
                    LocalDateTime.now());

            return "Recharge callback processed successfully";

        } catch (Exception e) {

            log.error("Recharge callback processing failed {}", e.getMessage(), e);

            commonFunctions.saveCallbackEventLog(
                    payload,
                    requestTime,
                    clientIp,
                    e.getMessage(),
                    LocalDateTime.now());

            throw new RuntimeException("Recharge callback failed");
        }

    }

    public void processCallback(RechargeCallbackData callback) {

        log.info("========== [RECHARGE CALLBACK RECEIVED] ==========");
        log.info("TransactionRef={}, Status={}", callback.getTransactionRef(), callback.getStatus());

        TransactionSchemeDetailsRequestDto dto = null;

        try {

            //  Fetch pending recharge transaction
            log.info("[1] Fetching recharge transaction {}", callback.getTransactionRef());

            RechargeTransaction txn =
                    rechargeTransactionRepository
                            .findByTransactionRef(callback.getTransactionRef())
                            .orElseThrow(() ->
                                    new RuntimeException("Recharge transaction not found: "
                                            + callback.getTransactionRef()));

            log.info("[1.1] Recharge txn fetched ID={}, Operator={}, Amount={}",
                    txn.getId(), txn.getTransactionType(), txn.getAmount());


            // Convert vendor status → internal status
            String internalStatus;

            if ("1".equals(callback.getStatus())) {
                internalStatus = "SUCCESS";
            } else if ("2".equals(callback.getStatus()) || "3".equals(callback.getStatus())) {
                internalStatus = "FAILED";
            } else if ("5".equals(callback.getStatus())) {
                internalStatus = "REFUNDED";
            } else if ("4".equals(callback.getStatus()) || "6".equals(callback.getStatus())) {
                internalStatus = "HOLD";
            } else {
                internalStatus = "PENDING";
            }

            log.info("[2] Vendor status {} mapped to internal status {}", callback.getStatus(), internalStatus);


            //  Update recharge transaction
            txn.setStatus(internalStatus);
            txn.setVendorTransactionId(callback.getVendorTxnId());
            txn.setOperatorTransactionId(callback.getOperatorTxnId());
            txn.setVendorResponse(callback.getStatusMessage());

            rechargeTransactionRepository.save(txn);

            log.info("[3] Recharge transaction updated successfully");

            //  Fetch matching SchemeRate
            Double txnAmt = txn.getAmount().doubleValue();

            log.info("[4] Searching SchemeRate schemeCode={}, operator={}, amount={}",
                    txn.getSchemeCode(), txn.getTransactionType(), txnAmt);

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            txn.getSchemeCode(),
                            txn.getTransactionType(),
                            txnAmt)
                    .orElseThrow(() ->
                            new RuntimeException("No scheme rate found for amount: " + txnAmt));

            log.info("[4.1] SchemeRate found ID={}, OperatorCode={}",
                    selectedScheme.getId(), selectedScheme.getOperatorCode());


            // Fetch scheme
            Scheme scheme = schemeRepository
                    .findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() ->
                            new RuntimeException("Scheme not found: " + selectedScheme.getSchemeCode()));

            log.info("[5] Scheme fetched hashCode={}", scheme.getSchemeCombinationHashCode());


            //  Parse schemeCombinationHashCode
            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

            if (hashParts.length < 2) {
                throw new RuntimeException(
                        "Invalid schemeCombinationHashCode: " + scheme.getSchemeCombinationHashCode());
            }

            short productId = Short.parseShort(hashParts[0]);
            String subProductId = hashParts[1];

            log.info("[6] ProductId={}, SubProductId={}", productId, subProductId);


            //  Fetch product and subproduct
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() ->
                            new RuntimeException("Product not found: " + productId));

            SubProducts subProduct = subProductsRepository.findById(subProductId)
                    .orElseThrow(() ->
                            new RuntimeException("SubProduct not found: " + subProductId));

            log.info("[7] Product={} SubProduct={}",
                    product.getName(), subProduct.getSubProductTitle());


            // Prepare DTO for transaction engine
            dto = new TransactionSchemeDetailsRequestDto();

            String baseTxnId = callback.getTransactionRef();

            if (baseTxnId.startsWith("TXN")) {
                baseTxnId = baseTxnId.substring(3);
            }

            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(txnAmt);
            dto.setConfirmAmount(txnAmt);
            dto.setOperatorName(txn.getTransactionType());
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
            dto.setHierarchyId(txn.getRetailerId());

            log.info("[8] DTO prepared for engine {}", dto);


            //  Call transaction engine
            transactionEngineServiceImpl.processTransaction(dto, internalStatus);

            log.info("[9] Transaction engine executed successfully");

        } catch (Exception ex) {

            log.error("[ERROR] Recharge callback processing failed for txn={} error={}",
                    callback.getTransactionRef(), ex.getMessage(), ex);
        }

        log.info("========== [RECHARGE CALLBACK PROCESS END] ==========");
    }


    @Override
    public MobileLookupResponse mobileLookup(
            MobileLookupRequest request,
            VendorCredentials credentials) {

        try {

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://roundpay.net/PlanServices/v1/MobileLookup")
                    .queryParam("accountNo", request.getMobileNumber())
                    .queryParam("spkey", "8201")
                    .queryParam("userid", "18022")
                    .queryParam("token", credentials.getTokenUrlUat())
                    .toUriString();

            log.info("Calling Mobile Lookup Vendor API");
            log.info("Vendor URL: {}", url);

            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            String.class
                    );

            log.info("Vendor Response Status: {}",
                    responseEntity.getStatusCode());

            String response = responseEntity.getBody();

            log.info("Vendor Raw Response: {}", response);

            if (response == null || response.isBlank()) {

                log.error("Empty response received from vendor");

                throw new RuntimeException(
                        "Empty response received from vendor"
                );
            }

            MobileLookupResponse mobileLookupResponse =
                    objectMapper.readValue(
                            response,
                            MobileLookupResponse.class
                    );

            log.info("Parsed Lookup Response: {}",
                    objectMapper.writeValueAsString(mobileLookupResponse));

            return mobileLookupResponse;

        } catch (HttpStatusCodeException ex) {

            log.error("Vendor HTTP Error Status: {}",
                    ex.getStatusCode());

            log.error("Vendor HTTP Error Response: {}",
                    ex.getResponseBodyAsString(), ex);

            throw new RuntimeException(
                    "Vendor API Error: " + ex.getResponseBodyAsString()
            );

        } catch (JsonProcessingException ex) {

            log.error("JSON Parsing Error", ex);

            throw new RuntimeException(
                    "Failed to parse lookup response: " + ex.getMessage()
            );

        } catch (Exception e) {

            log.error("Mobile Lookup Failed", e);

            throw new RuntimeException(
                    "Mobile lookup failed: " + e.getMessage()
            );
        }
    }

    @Override
    public OperatorResponse getOperatorCodes(VendorCredentials credentials) {

        String url = UriComponentsBuilder
                .fromHttpUrl("https://roundpay.net/PlanServices/v1/GetOperatorCodes")
                .queryParam("userid", "18022")
                .queryParam("token", credentials.getTokenUrlUat())
                .toUriString();

        String response = restTemplate.getForObject(url, String.class);

        try {
            return objectMapper.readValue(response, OperatorResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse operator response");
        }
    }

    @Override
    public RechargePlanResponse getRechargePlans(RechargePlanRequest request,
                                                 VendorCredentials credentials) {

        String url = UriComponentsBuilder
                .fromHttpUrl("https://roundpay.net/PlanServices/v1/RechargePlan")
                .queryParam("spkey", request.getSpkey())
                .queryParam("circleId", request.getCircleId())
                .queryParam("userid", "18022")
                .queryParam("token", credentials.getTokenUrlUat())
                .toUriString();

        String response = restTemplate.getForObject(url, String.class);

        try {
            return objectMapper.readValue(response, RechargePlanResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse recharge plans response");
        }
    }

    private int parseStatusCode(String vendorResponse) {

        String[] parts = vendorResponse.split("=");

        return Integer.parseInt(parts[0]);
    }

    private String mapStatus(int statusCode) {

        return switch (statusCode) {

            case 1 -> "SUCCESS";
            case 2, 3 -> "FAILED";
            case 4, 6 -> "HOLD";
            case 5 -> "REFUNDED";
            default -> "PENDING";
        };
    }

    private TransactionSchemeDetailsRequestDto buildTransactionDTO(
            RechargeTransaction txn,
            SchemeRate schemeRate,
            Scheme scheme,
            Products product,
            SubProducts subProduct) {

        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        String txnRef = txn.getTransactionRef();
        String baseTxnId = txnRef.startsWith("TXN") ? txnRef.substring(3) : txnRef;

        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txn.getAmount().doubleValue());
        dto.setConfirmAmount(txn.getAmount().doubleValue());
        dto.setOperatorName(txn.getTransactionType());
        dto.setOperatorCode(schemeRate.getOperatorCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(txn.getRetailerId());

        return dto;
    }
}

