package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.CellmobeyRechargeCallback;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.RechargeCallbackData;
import com.im.admin.response.RechargeResponse;
import com.im.admin.service.RechargeCallbackHandler;
import com.im.admin.service.RechargeVendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    ObjectMapper objectMapper = new ObjectMapper();

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

        //  Generate transaction reference
        String transactionRef = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        //  Build Message
        String message = request.getOperatorCode() + "$"
                + request.getCustomerNumber() + "$"
                + request.getAmount() + "$"
                + SMS_PIN + "$"
                + request.getRechargeType() + "$"
                + transactionRef;

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

        // Save transaction as PENDING before calling vendor
        RechargeTransaction transaction = RechargeTransaction.builder()
                .schemeCode(request.getSchemeCode())
                .transactionId(orderId)
                .schemeCode(request.getSchemeCode())
                .transactionRef(transactionRef)
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

// Split response
        String[] parts = vendorResponse.split("=");

        int statusCode = Integer.parseInt(parts[0]);
        String statusMessage = parts.length > 1 ? parts[1] : "Unknown Response";

// If vendor rejected request
        if (statusCode != 0) {

            transaction.setStatus("FAILED");
            rechargeTransactionRepository.save(transaction);

            return RechargeResponse.builder()
                    .transactionRef(transactionRef)
                    .status("FAILED")
                    .message(statusMessage)
                    .build();
        }

// If vendor accepted request
        transaction.setStatus("PENDING");
        rechargeTransactionRepository.save(transaction);

        return RechargeResponse.builder()
                .transactionRef(transactionRef)
                .status("PENDING")
                .message(statusMessage)   // show vendor message
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


            // ⃣ Convert vendor status → internal status
            String internalStatus;

            if ("1".equals(callback.getStatus())) {
                internalStatus = "SUCCESS";
            }
            else if ("2".equals(callback.getStatus()) || "3".equals(callback.getStatus())) {
                internalStatus = "FAILED";
            }
            else if ("5".equals(callback.getStatus())) {
                internalStatus = "REFUNDED";
            }
            else if ("4".equals(callback.getStatus()) || "6".equals(callback.getStatus())) {
                internalStatus = "HOLD";
            }
            else {
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
            Double txnAmt =txn.getAmount().doubleValue();

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

        }
        catch (Exception ex) {

            log.error("[ERROR] Recharge callback processing failed for txn={} error={}",
                    callback.getTransactionRef(), ex.getMessage(), ex);
        }

        log.info("========== [RECHARGE CALLBACK PROCESS END] ==========");
    }
}

