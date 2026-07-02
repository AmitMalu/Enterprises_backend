package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.BbpsTranactionRequestDto;
import com.im.admin.dto.BillRechargeRequestDTO;
import com.im.admin.dto.RetailersExtraColumnsDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.BillFetchRequest;
import com.im.admin.request.BillPaymentRequest;
import com.im.admin.request.BillerRequest;
import com.im.admin.request.TransactionStatusRequest;
import com.im.admin.response.BillAvenueBillPayResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BillAvenueService;
import com.im.admin.service.DmtVendorService;
import com.im.admin.util.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BillAvenueServiceImpl implements BillAvenueService {

    @Autowired
    private BillPayConfigRepository billPayConfigRepository;


    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;


    @Autowired
    private BBPSRelatedTransactions bbpsRelatedTransactions;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private DmtVendorFactory vendorFactory;

    @Autowired
    private SubProductsRepository subProductsRepository;


    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private DmtTransactionRepository dmtTransactionRepository;

    @Autowired
    private RemitterRepository remitterRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private BillAvenueTransactionRepository billAvenueTransactionRepository;

    @Autowired
    private BBPSPendingTransactionsRepository bbpsPendingTransactionsRepository;

    // old credentials
//    private static final String ACCESS_CODE = "AVUU30TF96SP77ZJJH";
//    private static final String INSTITUTE_ID = "NF07";
//    private static final String WORKING_KEY = "063F0699C1D46414EA1E92D0E6BDCF99";

  // line credentials
    private static final String ACCESS_CODE = "AVWR10SG05FK02ERLL";
    private static final String INSTITUTE_ID = "RP98";
    private static final String WORKING_KEY = "B085170FB32233C416BA61E70E25C86E";


    private static final String API_URL =
            "https://api.billavenue.com/billpay/extMdmCntrl/mdmRequestNew/json";

    @Override
    public Object fetchBillerInfobulk(BillerRequest request) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        List<String> billerIds = request.getBillerId();

        String requestId = RequestIdUtil.generate();
        //Check in DB before calling vendor API
        List<BillPayConfig> existingConfigs =
                billPayConfigRepository.findByProviderIdIn(billerIds);

        if (existingConfigs.size() == billerIds.size()) {

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode billerArrayNode = mapper.createArrayNode();

            for (BillPayConfig cfg : existingConfigs) {
                billerArrayNode.add(mapper.readTree(cfg.getRawConfig()));
            }
            ObjectNode responseNode = mapper.createObjectNode();
            responseNode.set("biller", billerArrayNode);
            responseNode.put("requestId", requestId);

            return CommonResponse.success(responseNode, "Biller info fetched");
        }
        // NOT FOUND → Hit vendor API
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);
        vendorId = vendor.getId();
        ObjectMapper mapper = new ObjectMapper();

        //  Convert request → JSON string
        String plainJson = mapper.writeValueAsString(request);
        requestJson = plainJson;

        //  Encrypt
        String encryptedHex = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);
        //  requestId
        String requestIds = RequestIdUtil.generate();
        //  Build API URL
        String finalUrl = API_URL
                + "?accessCode=" + ACCESS_CODE
                + "&requestId=" + requestIds
                + "&ver=1.0"
                + "&instituteId=" + INSTITUTE_ID;

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> entity = new HttpEntity<>(encryptedHex, headers);
        try {
            //  Call API
            RestTemplate restTemplate = new RestTemplate();
            String encryptedResponse = restTemplate.postForObject(finalUrl, entity, String.class);
            //  Decrypt
            String decrypt = BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);
            //  Convert decrypted text → JSON
            JsonNode jsonNode = mapper.readTree(decrypt);
            ObjectNode responseNode = (ObjectNode) jsonNode;
                  // add requestId
            responseNode.put("requestId", requestIds);
            responseJson = responseNode.toString();

//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    responseJson,
//                    null
//            );
            // Save into DB
            JsonNode billerArray = jsonNode.path("biller");

            if (billerArray.isArray() && billerArray.size() > 0) {
                List<BillPayConfig> configs = new ArrayList<>();

                for (JsonNode biller : billerArray) {

                    BillPayConfig cfg = new BillPayConfig();
                    cfg.setVendor(vendor.getVendorName());
                    cfg.setServiceName(biller.path("billerCategory").asText());
                    cfg.setProviderId(biller.path("billerId").asText());
                    cfg.setProviderName(biller.path("billerName").asText());
                    cfg.setRawConfig(biller.toString());

                    configs.add(cfg);
                }
                billPayConfigRepository.saveAll(configs);
            }
            return CommonResponse.success(responseNode, "Biller info fetched from Vendor API");
        } catch (Exception e) {

//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    null,
//                    e.getMessage()
//            );
            throw e;
        }
    }


    @Override
    public Object fetchBillerInfo(BillerRequest request) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


        String requestedBillerId = request.getBillerId().get(0); // first biller id

        String requestId = RequestIdUtil.generate();
        // ----------------------------
        // 1. Check in DB before calling vendor API

        // ----------------------------
        Optional<BillPayConfig> existing =
                billPayConfigRepository.findByProviderId(requestedBillerId);

        if (existing.isPresent()) {

            // Return saved raw_config JSON (convert to JsonNode)
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode savedNode = (ObjectNode) mapper.readTree(existing.get().getRawConfig());
            savedNode.put("requestId", requestId);

            return CommonResponse.success(savedNode, "Biller info fetched");
        }


        // ----------------------------
        // 2. NOT FOUND → Hit vendor API
        // ----------------------------

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        vendorId = vendor.getId();


        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Convert request → JSON string
        String plainJson = mapper.writeValueAsString(request);

        requestJson = plainJson;

        // Step 2: Encrypt
        String encryptedHex = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

        // Step 3: requestId
        String requestIds = RequestIdUtil.generate();

        // Step 4: Build API URL
        String finalUrl = API_URL
                + "?accessCode=" + ACCESS_CODE
                + "&requestId=" + requestIds
                + "&ver=1.0"
                + "&instituteId=" + INSTITUTE_ID;

        // Step 5: Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> entity = new HttpEntity<>(encryptedHex, headers);

        try {
            // Step 6: Call API
            RestTemplate restTemplate = new RestTemplate();
            String encryptedResponse = restTemplate.postForObject(finalUrl, entity, String.class);

            // Step 7: Decrypt
            String decrypt = BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

            // Step 8: Convert decrypted text → JSON
            JsonNode jsonNode = mapper.readTree(decrypt);

            ObjectNode responseNode = (ObjectNode) jsonNode;

// add requestId
            responseNode.put("requestId", requestIds);

            responseJson = responseNode.toString();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );


            // ----------------------------
            // 3. Save into DB
            // ----------------------------

            JsonNode billerArray = jsonNode.path("biller");

            if (billerArray.isArray() && billerArray.size() > 0) {

                JsonNode biller = billerArray.get(0);

                BillPayConfig cfg = new BillPayConfig();

                cfg.setVendor(vendor.getVendorName());
                cfg.setServiceName(biller.path("billerCategory").asText());
                cfg.setProviderId(biller.path("billerId").asText());
                cfg.setProviderName(biller.path("billerName").asText());
                cfg.setRawConfig(biller.toString());

                billPayConfigRepository.save(cfg);
            }

            return CommonResponse.success(responseNode, "Biller info fetched from Vendor API");
        } catch (Exception e) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    e.getMessage()
            );

            throw e;
        }
    }


    @Override
    public Object fetchBill(BillFetchRequest request) throws Exception {


        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        ObjectMapper mapper = new ObjectMapper();
        // uat
//        request.setAgentId("CC01CC01513515340681");
        //live
        request.setAgentId("CC01RP98AGTBAK020158");

        // ================================
        BillFetchRequest.AgentDeviceInfo deviceInfo =
                request.getAgentDeviceInfo();

        if (deviceInfo == null) {
            deviceInfo = new BillFetchRequest.AgentDeviceInfo();
        }

        deviceInfo.setIp("192.168.2.73");
        deviceInfo.setInitChannel("AGT");
        deviceInfo.setMac("01-23-45-67-89-ab");

        request.setAgentDeviceInfo(deviceInfo);

        // ================================
        BillFetchRequest.CustomerInfo customerInfo =
                request.getCustomerInfo();

        if (customerInfo == null) {
            customerInfo = new BillFetchRequest.CustomerInfo();
        }

        customerInfo.setCustomerEmail("");
        customerInfo.setCustomerAdhaar("");
        customerInfo.setCustomerPan("");

        request.setCustomerInfo(customerInfo);


        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        vendorId = vendor.getId();

        // Step 1: Convert request to JSON string
        String plainJson = mapper.writeValueAsString(request);
        requestJson = plainJson;


        // Step 2: Encrypt JSON
        String encryptedRequest = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

        // Step 3: Generate requestId
//        String requestId = RequestIdUtil.generate();
        String requestId = request.getRequestId();

        // Step 4: Prepare final URL (encRequest included as query param)
        String finalUrl =
                "https://api.billavenue.com/billpay/extBillCntrl/billFetchRequest/j" +
                        "son"
                        + "?accessCode=" + ACCESS_CODE
                        + "&requestId=" + requestId
                        + "&ver=1.0"
                        + "&instituteId=" + INSTITUTE_ID
                        + "&encRequest=" + encryptedRequest;   //  URL PARAM

        // Step 5: Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        //  Empty POST body
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
        // Step 6: POST API call (EMPTY BODY)
        String encryptedResponse = restTemplate.postForObject(
                finalUrl,
                entity,
                String.class
        );

        // Step 7: Decrypt response
        String decrypted = BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

        JsonNode responseNode = mapper.readTree(decrypted);

        responseJson = responseNode.toString();

        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                responseJson,
                null
        );



        // Step 8: Convert decrypted JSON to object
        return mapper.readTree(decrypted);
    }
     catch (Exception e) {

        // ================================
        //  FAILURE VENDOR LOG
        // ================================
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                null,
                e.getMessage()
        );

        throw e;
    }
}


@Transactional
    @Override
    public Object doBillPayment(BillPaymentRequest request, Double latitude, Double longitude, String ipAddress) throws Exception {

        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS BILL PAYMENT] Starting transaction | orderId={}", orderId);

        String baseTransactionId = commonMethods.generateBaseTransactionId();

    LocalDateTime requestTime = LocalDateTime.now();
    String requestJson = null;
    String responseJson = null;
    String vendorId = null;

        try {
            // ⃣ Validate SubProduct
            SubProducts subProducts = subProductsRepository.findById(request.getSubProductId())
                    .orElseThrow(() ->
                            new RuntimeException("SubProduct not found for ID: " + request.getSubProductId()));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

            // 2️ Balance Check
            BigDecimal availableBalance =
                    commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());

            BigDecimal amount = request.getAmountInfo().getAmount();

            if (availableBalance.compareTo(amount) < 0) {
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            // 3 Vendor Routing
            VendorRouting routing =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(routing);

            vendorId = vendor.getId();

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            String retailerId = request.getRetailerId();

            // 4 Wallet Debit (PENDING)
            AbstractWalletAmountTable walletAmountTable =
                    commonMethods.getWalletByType(walletType.name(), retailerId);

            Products product = subProducts.getProduct();

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            request.getSchemeCode(),
                            request.getTxnType(),
                            amount.doubleValue())
                    .orElseThrow(() -> new RuntimeException("No slab found for the given amount: " + amount));

            BbpsTranactionRequestDto bbpsDto =
                    new BbpsTranactionRequestDto(
                            selectedScheme,
                            amount.doubleValue(),
                            walletType.name(),
                            retailerId,
                            String.valueOf(product.getSgst()),
                            String.valueOf(product.getTds()),
                            "PENDING",
                            baseTransactionId,
                            selectedScheme.getOperatorName(),
                            subProducts.getSubProductTitle(),
                            latitude,
                            longitude,
                            "Bill Avenue",
                            "Not Yet"
                    );

            RetailersExtraColumnsDto retailerExtraColumns =
                    RetailersExtraColumnsDto.builder()
                            .medium(request.getMedium())
                            .clientIp(ipAddress)
                            .requestDomainName(request.getRequestDomainName())
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();

            commonMethods.updateWallet(
                    walletAmountTable,
                    retailerId,
                    walletType,
                    amount,
                    "Debit",
                    baseTransactionId,
                    "PENDING",
                    "DEBIT",
                    subProducts.getSubProductTitle(),
                    "Bill Avenue BillPay",
                    "Bill payment initiated",
                    "",
                    retailerExtraColumns
            );

            bbpsRelatedTransactions.processCharges(bbpsDto, walletAmountTable);

            //live
            request.setAgentId("CC01RP98AGTBAK020158");

            request.setPaymentRefId("45kfpjsc4e0c9fe9pohee94101");

            BillPaymentRequest.AgentDeviceInfo deviceInfo =
                    request.getAgentDeviceInfo();

            if (deviceInfo == null) {
                deviceInfo = new BillPaymentRequest.AgentDeviceInfo();
            }

            deviceInfo.setIp("192.168.2.73");
            deviceInfo.setInitChannel("AGT");
            deviceInfo.setMac("01-23-45-67-89-ab");

            request.setAgentDeviceInfo(deviceInfo);

            // ================================
            BillPaymentRequest.CustomerInfo customerInfo =
                    request.getCustomerInfo();

            if (customerInfo == null) {
                customerInfo = new BillPaymentRequest.CustomerInfo();
            }

            customerInfo.setCustomerEmail("");
            customerInfo.setCustomerAdhaar("");
            customerInfo.setCustomerPan("");

            request.setCustomerInfo(customerInfo);

            //multiply by 100 and give to vendor code
            BillPaymentRequest vendorRequest = new BillPaymentRequest();
            BeanUtils.copyProperties(request, vendorRequest);

             //  DEEP COPY AmountInfo
            BillPaymentRequest.AmountInfo amountInfo =
                    new BillPaymentRequest.AmountInfo();
            BeanUtils.copyProperties(request.getAmountInfo(), amountInfo);

             // multiply for vendor
            amountInfo.setAmount(
                    amountInfo.getAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(0, RoundingMode.UNNECESSARY)
            );


            // set back to vendorRequest
            vendorRequest.setAmountInfo(amountInfo);

            log.info("REQ  AMT = {}", request.getAmountInfo().getAmount());
            log.info("VEND AMT = {}", vendorRequest.getAmountInfo().getAmount());

            // 5 Encrypt Request
            ObjectMapper mapper = new ObjectMapper();

            String plainJson = mapper.writeValueAsString(vendorRequest);
            requestJson = plainJson;

            String encryptedRequest = BillAvenueCryptoUtil.encrypt(plainJson, WORKING_KEY);

            String requestId = request.getRequestId();

            String finalUrl =
                    "https://api.billavenue.com/billpay/extBillPayCntrl/billPayRequest/json"
                            + "?accessCode=" + ACCESS_CODE
                            + "&requestId=" + requestId
                            + "&ver=1.0"
                            + "&instituteId=" + INSTITUTE_ID
                            + "&encRequest=" + encryptedRequest;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            RestTemplate restTemplate = new RestTemplate();

            // 6⃣ Call Vendor API
            String encryptedResponse =
                    restTemplate.postForObject(finalUrl, entity, String.class);

            String decrypted =
                    BillAvenueCryptoUtil.decrypt(encryptedResponse, WORKING_KEY);

            responseJson = decrypted;

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            BillAvenueBillPayResponse vendorResponse =
                    mapper.readValue(decrypted, BillAvenueBillPayResponse.class);

            String txnId = buildTransactionId("TXN", baseTransactionId);

            // save vendor response in db
            BillAvenueTransaction transaction = new BillAvenueTransaction();

            transaction.setCreatedDateTime(LocalDateTime.now());
            String txnStatus;
            if ("000".equals(vendorResponse.getResponseCode())) {
                txnStatus = "SUCCESS";
            } else if ("002".equals(vendorResponse.getResponseCode())
                    || "FORWARD".equalsIgnoreCase(vendorResponse.getTxnRespType())) {
                txnStatus = "PENDING";
            } else {
                txnStatus = "FAILED";
            }
            transaction.setTxnStatus(txnStatus);

            String billerId = request.getBillerId();

            BillPayConfig config = billPayConfigRepository
                    .findByProviderId(billerId)
                    .orElseThrow(() ->
                            new RuntimeException("No BillPayConfig found for providerId: " + billerId)
                    );
            transaction.setBillerName(config.getProviderName());

            long amt = 0L;

            if (vendorResponse.getRespAmount() != null && !vendorResponse.getRespAmount().isEmpty()) {
                amt = Long.parseLong(vendorResponse.getRespAmount()) / 100;
            } else {
                log.warn("Vendor response amount is null for txnId={}", baseTransactionId);
            }
            transaction.setTxnAmount(amt);
            transaction.setCustomerName(vendorResponse.getRespCustomerName());
            transaction.setConsumerNumber(request.getNumber());
            transaction.setCategory(request.getTxnType());
            transaction.setTxnId(txnId);

            transaction.setReferenceNumber(
                    vendorResponse.getApprovalRefNumber()
            );
            billAvenueTransactionRepository.save(transaction);

            // ================= SUCCESS FLOW =================
            if ("000".equals(vendorResponse.getResponseCode())) {

                bbpsRelatedTransactions.updateTransactionStatus(
                        retailerId,
                        walletType.name(),
                        baseTransactionId
                );

                bbpsDto.setVendorResponse("SUCCESS");
                bbpsRelatedTransactions.processCommission(bbpsDto);

                return CommonResponse.success(
                        vendorResponse,
                        "Transaction Successful"
                );
            }



            // ================= FAILURE FLOW =================

                if ("002".equals(vendorResponse.getResponseCode())
                        || "FORWARD".equalsIgnoreCase(vendorResponse.getTxnRespType())) {

                    savePendingTransaction(
                        baseTransactionId,
                        retailerId,
                        walletType,
                        request,
                        selectedScheme,
                        String.valueOf(product.getSgst()),
                        String.valueOf(product.getTds()),
                        selectedScheme.getOperatorName(),
                        subProducts,
                        credentials,
                        vendorResponse.getVErrorRootVO().getError().get(0).getErrorMessage(),
                        vendorResponse.getVErrorRootVO().getError().get(0).getErrorMessage(),
                        vendorResponse.getTxnRefId()
                );
                return CommonResponse.success(
                        vendorResponse,
                        "Transaction is pending. Please check status later."
                );
            }

            /* ---------- FAILURE CASE ---------- */
            bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(
                    retailerId,
                    walletType.name(),
                    baseTransactionId
            );

            return new CommonResponse<>(400, "Transaction Failed", vendorResponse);

        } catch (Exception ex) {

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[PROCESS BILL PAYMENT] Exception | baseTxnId={}", baseTransactionId, ex);

            bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(
                    request.getRetailerId(),
                    WalletType.valueOf(
                            subProductsRepository
                                    .findById(request.getSubProductId())
                                    .get()
                                    .getWalletType()
                    ).name(),
                    baseTransactionId
            );

            throw ex;
        }
}

    private void savePendingTransaction(
            String baseTransactionId,
            String retailerId,
            WalletType walletType,
            BillPaymentRequest request,
            SchemeRate schemeRate,
            String gstValue,
            String tdsValue,
            String operator,
            SubProducts subProduct,
            VendorCredentials credentials,
            String status,
            String actualVendorStatus,
            String vendorTxnRefId
    ) {
        try {
            BBPSPendingTransactions pending = new BBPSPendingTransactions();
            pending.setTransactionId("TXN" + baseTransactionId);
            pending.setRetailerId(retailerId);
            pending.setWalletType(walletType.name());
            pending.setAmount(request.getAmountInfo().getAmount());
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
//            pending.setVendorTxnRefId(vendorTxnRefId);     // ** set when u do for production and add field also (after add this we can get tranrefid in billAvenueShedulaer)

            bbpsPendingTransactionsRepository.save(pending);
            log.info("Saved pending transaction with status [{}]: {}", status, baseTransactionId);

        } catch (Exception e) {
            log.error("Failed to save pending transaction [{}]: {}", baseTransactionId, e.getMessage(), e);
            throw new RuntimeException("Error saving pending transaction: " + e.getMessage());
        }
    }

    @Override
    public Object fetchTransactionStatus(TransactionStatusRequest request) throws Exception {

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);


        ObjectMapper mapper = new ObjectMapper();

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = vendor.getId();

        try {
            // ===== Step 1 Prepare plain JSON =====
            requestJson = mapper.writeValueAsString(request);

            // ===== Step 2 Encrypt request =====
            String encryptedRequest =
                    BillAvenueCryptoUtil.encrypt(requestJson, WORKING_KEY);

            // ===== Step 3 RequestId =====
            String requestId =
                    request.getRequestId() != null
                            ? request.getRequestId()
                            : RequestIdUtil.generate();

            // ===== Step 4 Build Final URL =====
            String finalUrl =
                    "https://stgapi.billavenue.com/billpay/transactionStatus/fetchInfo/json"
                            + "?accessCode=" + ACCESS_CODE
                            + "&requestId=" + requestId
                            + "&ver=1.0"
                            + "&instituteId=" + INSTITUTE_ID
                            + "&encRequest=" + encryptedRequest;

            // ===== Step 5 Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            RestTemplate restTemplate = new RestTemplate();

            // ===== Step 6 Call Vendor API =====
            String encryptedResponse =
                    restTemplate.postForObject(
                            finalUrl,
                            entity,
                            String.class
                    );

            // ===== Step 7 Decrypt response =====
            responseJson =
                    BillAvenueCryptoUtil.decrypt(
                            encryptedResponse,
                            WORKING_KEY
                    );

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // ===== Step 8 Return JSON =====
            return mapper.readTree(responseJson);

        } catch (Exception ex) {

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[FETCH TXN STATUS] Exception | requestId={}",
                    request.getRequestId(), ex);

            throw ex;
        }
    }


    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }
}


