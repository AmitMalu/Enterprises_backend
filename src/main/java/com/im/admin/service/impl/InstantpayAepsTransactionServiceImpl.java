package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.InstantpayResponseMapper;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.InstantPayCashWithdrawalRequestDto;
import com.im.admin.request.InstantpayBalanceEnquiryRequestDto;
import com.im.admin.request.InstantpayMiniStatementRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.InstantpayBalanceEnquiryResponseDto;
import com.im.admin.response.InstantpayCashWithdrawalResponseDto;
import com.im.admin.response.InstantpayMiniStatementResponseDto;
import com.im.admin.service.InstantpayAepsTransactionService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.InstantpayEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InstantpayAepsTransactionServiceImpl implements InstantpayAepsTransactionService {

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;


    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private AepsInstantPayMerchantStatusRepository aepsInstantPayMerchantStatusRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private  AepsMiniStatementRepository aepsMiniStatementRepository;

    @Autowired
    private AepsBalanceEnquiryRepository aepsBalanceEnquiryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;


    @Autowired
    private AepsCashWithdrawalRepository cashWithdrawalRepository;


    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;


    @Override
    public CommonResponse<InstantpayCashWithdrawalResponseDto> cashWithdrawal(InstantPayCashWithdrawalRequestDto req,Double latitude,Double longitude) {
        try {

            LocalDateTime requestTime = LocalDateTime.now();
            String responseJson = null;

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            String orderId = baseTransactionId;
            log.info("[AEPS BE] Start | orderId={}", orderId);

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));


            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String hardCodeInput = "CW";

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));

            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "CW"
            );

            String baseUrl = credentials.getBaseUrlProd();
            String saltKey = credentials.getSaltKey();

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

//            String outletId = merchant.getMerchantLoginId();
            String outletId = merchant.getOutletId();

            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            req.getAadhaarNumber(),
                            saltKey);

            String externalRef = "AEPS-" + req.getRetailerId() + "-" + System.currentTimeMillis();

            String url = baseUrl + "/fi/aeps/cashWithdrawal";

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("bankiin", req.getBankiin());
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("mobile", req.getMobile());
            body.put("amount", req.getAmount());
            body.put("externalRef", externalRef);
            body.put("captureType", "FINGER");

            Map<String, Object> bio = new LinkedHashMap<>();

            bio.put("encryptedAadhaar", encryptedAadhaar);
            bio.put("dc", req.getBiometricData().getDc());
            bio.put("ci", req.getBiometricData().getCi());
            bio.put("hmac", req.getBiometricData().getHmac());
            bio.put("dpId", req.getBiometricData().getDpID());
            bio.put("mc", req.getBiometricData().getMc());
            bio.put("pidDataType", req.getBiometricData().getPidDatatype());
            bio.put("sessionKey", req.getBiometricData().getSessionKey());
            bio.put("mi", req.getBiometricData().getMi());
            bio.put("rdsId", req.getBiometricData().getRdsID());
            bio.put("errCode", req.getBiometricData().getErrCode());
            bio.put("errInfo", req.getBiometricData().getErrInfo());
            bio.put("fCount", req.getBiometricData().getFCount());
            bio.put("fType", req.getBiometricData().getFType());
            bio.put("iCount", req.getBiometricData().getICount());
            bio.put("iType", req.getBiometricData().getIType());
            bio.put("pCount", req.getBiometricData().getPCount());
            bio.put("pType", req.getBiometricData().getPType());
            bio.put("srno", req.getBiometricData().getSrno());
            bio.put("sysid", req.getBiometricData().getSysid());
            bio.put("ts", req.getBiometricData().getTs());
            bio.put("pidData", req.getBiometricData().getPiddata());
            bio.put("qScore", req.getBiometricData().getQScore());
            bio.put("nmPoints", req.getBiometricData().getNmPoints());
            bio.put("rdsVer", req.getBiometricData().getRdsVer());

            body.put("biometricData", bio);

            ObjectMapper mapper = new ObjectMapper();
            String requestJson = mapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<InstantpayCashWithdrawalResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayCashWithdrawalResponseDto.class
                    );


            InstantpayCashWithdrawalResponseDto vendorResp = response.getBody();

            responseJson = mapper.writeValueAsString(vendorResp);

            // ===== SAVE VENDOR LOG =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (vendorResp == null || vendorResp.getData() == null) {
                return CommonResponse.error(500, "Vendor returned empty response");
            }

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

            String txnId;
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("TXN", baseTxnId);
            } else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("PND", baseTxnId);
            } else {
                txnId = buildTransactionId("FLD", baseTxnId);
            }

            // ===============================
            // SAVE TRANSACTION LOG
            // ===============================
//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())) {
            AepsCashWithdrawalLog logEntry = new AepsCashWithdrawalLog();
            logEntry.setRetailerId(
                    req.getRetailerId() != null ? req.getRetailerId() : null
            );

            logEntry.setVendorId(
                    vendor.getId() != null ? vendor.getId() : null
            );

            logEntry.setMerchantTxnId(
                    externalRef != null ? externalRef : null
            );

// logEntry.setTransactionId(vendorResp.getOrderid());
            logEntry.setTransactionId(
                    txnId != null ? txnId : null
            );

            String transactionValue = vendorResp.getData().getTransactionValue();
            logEntry.setTransactionAmount(
                    (transactionValue != null && !transactionValue.trim().isEmpty())
                            ? Double.valueOf(transactionValue)
                            : null
            );

            String closingBalance = vendorResp.getData().getClosingBalance();
            logEntry.setBalanceAmount(
                    (closingBalance != null && !closingBalance.trim().isEmpty())
                            ? Double.valueOf(closingBalance)
                            : null
            );

            logEntry.setBankName(
                    vendorResp.getData().getBankName() != null
                            ? vendorResp.getData().getBankName()
                            : null
            );

            logEntry.setTransactionStatus(
                    vendorResp.getStatuscode() != null
                            ? vendorResp.getStatuscode()
                            : null
            );

            logEntry.setStatusMessage(
                    vendorResp.getStatus() != null
                            ? vendorResp.getStatus()
                            : null
            );

            logEntry.setRequestPayload(
                    requestJson != null
                            ? requestJson
                            : null
            );

            String responsePayload = mapper.writeValueAsString(vendorResp);
            logEntry.setResponsePayload(
                    responsePayload != null
                            ? responsePayload
                            : null
            );

            cashWithdrawalRepository.save(logEntry);
//            }

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getTransactionAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();  //BE
            dto.setBaseTranxId(baseTxnId);
//            dto.setAmount(req.getTransactionAmount().doubleValue());
            dto.setAmount(req.getAmount().doubleValue());
//            dto.setConfirmAmount(req.getTransactionAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName("BE");
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);

            Products product = subProducts.getProduct();
//            dto.setTransactionType(product.getTransactionType());
            dto.setTransactionType("Credit");
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());
            dto.setLatitude(Double.valueOf(latitude));
            dto.setLongitude(Double.valueOf(longitude));

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(200, "Transaction Successful", vendorResp);
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", vendorResp);
            }
            else {
                return new CommonResponse<>(400, "Transaction Failed", vendorResp);
            }

        } catch (Exception e) {

            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }
    }

    @Override
    public CommonResponse<InstantpayBalanceEnquiryResponseDto> balanceEnquiry(InstantpayBalanceEnquiryRequestDto req, Double latitude, Double longitude) {

        try {
            log.info("========== AEPS BALANCE ENQUIRY START ==========");

            log.info("Finding SubProduct | SubProductId={}", req.getSubProductId());

            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            log.info("SubProduct Found | Id={}, Code={}, Title={}",
                    subProducts.getId(),
                    subProducts.getSubProductCode(),
                    subProducts.getSubProductTitle());

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated BaseTransactionId={}", baseTransactionId);

            String orderId = baseTransactionId;
            log.info("[AEPS BE] Start | OrderId={}", orderId);

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;
            log.info("BaseTxnId={}", baseTxnId);

            LocalDateTime requestTime = LocalDateTime.now();
            log.info("RequestTime={}", requestTime);

            String requestJson = null;
            String responseJson = null;

            log.info("Finding Retailer | RetailerId={}", req.getRetailerId());

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            log.info("Retailer Found | Id={}, Name={}, Latitude={}, Longitude={}",
                    retailer.getName(),
                    retailer.getLatitude(),
                    retailer.getLongitude());

            log.info("Finding Vendor Routing | SubProductId={}", req.getSubProductId());

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            log.info("Vendor Routing={}", vendorRouting);

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            log.info("Vendor Found | VendorId={}, ProductId={}, ProductName={}",
                    vendor.getId(),
                    vendor.getProduct().getId(),
                    vendor.getProduct().getName());

            log.info("Finding Vendor Credentials | VendorId={}, ProductId={}",
                    vendor.getId(),
                    vendor.getProduct().getId());

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            log.info("Vendor Credentials Found | ClientId={}, BaseUrl={}",
                    credentials.getClientId(),
                    credentials.getBaseUrlProd());

            String hardCodeInput = "BE";
            log.info("Finding Operator | HardCode={}", hardCodeInput);

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));

            log.info("Operator Found | Id={}, Name={}",
                    operator.getId(),
                    operator.getOperatorName());

            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "BE"
            );

            log.info("TransactionType Set={}", req.getTransactionType());

            String baseUrl = credentials.getBaseUrlProd();
            String saltKey = credentials.getSaltKey();

            log.info("BaseUrl={}", baseUrl);
            log.info("SaltKey Present={}", saltKey != null);

            log.info("Finding Merchant | RetailerId={}", req.getRetailerId());

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            log.info("Merchant Found | MerchantId={}, OutletId={}",
                    merchant.getId(),
                    merchant.getOutletId());

//        String outletId = merchant.getMerchantLoginId();
            String outletId = merchant.getOutletId();

            log.info("OutletId={}", outletId);

            log.info("Encrypting Aadhaar");
            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            req.getAadhaarNumber(),
                            saltKey);

            log.info("Aadhaar Encrypted Successfully");

            String externalRef =
                    "AEPS-BAL-" + req.getRetailerId() + "-" + System.currentTimeMillis();

            log.info("ExternalRef={}", externalRef);

            String url = baseUrl + "/fi/aeps/balanceInquiry";

            log.info("Vendor URL={}", url);

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("bankiin", req.getBankiin());
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("mobile", req.getMobile());
            body.put("externalRef", externalRef);
            body.put("captureType", "FINGER");

            log.info("Request Body Basic Fields Added");

            Map<String, Object> bio = new LinkedHashMap<>();

            bio.put("encryptedAadhaar", encryptedAadhaar);
            bio.put("dc", req.getBiometricData().getDc());
            bio.put("ci", req.getBiometricData().getCi());
            bio.put("hmac", req.getBiometricData().getHmac());
            bio.put("dpId", req.getBiometricData().getDpID());
            bio.put("mc", req.getBiometricData().getMc());
            bio.put("pidDataType", req.getBiometricData().getPidDatatype());
            bio.put("sessionKey", req.getBiometricData().getSessionKey());
            bio.put("mi", req.getBiometricData().getMi());
            bio.put("rdsId", req.getBiometricData().getRdsID());
            bio.put("errCode", req.getBiometricData().getErrCode());
            bio.put("errInfo", req.getBiometricData().getErrInfo());
            bio.put("fCount", req.getBiometricData().getFCount());
            bio.put("fType", req.getBiometricData().getFType());
            bio.put("iCount", req.getBiometricData().getICount());
            bio.put("iType", req.getBiometricData().getIType());
            bio.put("pCount", req.getBiometricData().getPCount());
            bio.put("pType", req.getBiometricData().getPType());
            bio.put("srno", req.getBiometricData().getSrno());
            bio.put("sysid", req.getBiometricData().getSysid());
            bio.put("ts", req.getBiometricData().getTs());
            bio.put("pidData", req.getBiometricData().getPiddata());
            bio.put("qScore", req.getBiometricData().getQScore());
            bio.put("nmPoints", req.getBiometricData().getNmPoints());
            bio.put("rdsVer", req.getBiometricData().getRdsVer());

            log.info("Biometric Data Prepared");

            body.put("biometricData", bio);

            log.info("Final Request Body Prepared");

            ObjectMapper mapper = new ObjectMapper();

            requestJson = mapper.writeValueAsString(body);

            log.info("Request JSON={}", requestJson);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());
            headers.set("X-Ipay-Outlet-Id", outletId);

            log.info("Headers Prepared | ClientId={}, OutletId={}, EndpointIp={}",
                    credentials.getClientId(),
                    outletId,
                    retailer.getCreatedByIPAddress());

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            log.info("Calling InstantPay Balance Enquiry API");

            ResponseEntity<InstantpayBalanceEnquiryResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayBalanceEnquiryResponseDto.class
                    );

            log.info("Vendor API Response Status={}", response.getStatusCode());

            InstantpayBalanceEnquiryResponseDto vendorResp = response.getBody();

            log.info("Vendor Response Body={}", vendorResp);

            responseJson = mapper.writeValueAsString(vendorResp);

            log.info("Response JSON={}", responseJson);

// ===== SAVE VENDOR LOG =====
            log.info("Saving Vendor Logs");

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            log.info("Vendor Logs Saved Successfully");

            if (vendorResp == null || vendorResp.getData() == null) {

                log.error("Vendor Response is NULL or Data is NULL");
                log.error("Vendor Response={}", vendorResp);

                return new CommonResponse<>(
                        500,
                        "Vendor returned empty response",
                        null
                );
            }

            log.info("Vendor Response Valid");

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

            String txnId;
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("TXN", baseTxnId);
            } else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("PND", baseTxnId);
            } else {
                txnId = buildTransactionId("FLD", baseTxnId);
            }

// ===== SAVE AEPS BALANCE LOG =====
//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())
//                    && vendorResp.getData() != null) {
            log.info("Preparing AEPS Balance Enquiry Log");
            AepsBalanceEnquiry logEntry = new AepsBalanceEnquiry();

            logEntry.setRetailerId(req.getRetailerId());
            logEntry.setVendorId(vendor.getId());
            logEntry.setMerchantTxnId(externalRef);
// logEntry.setTransactionId(vendorResp.getOrderid());
            logEntry.setTransactionId(txnId);

            String bankAccountBalance = vendorResp.getData().getBankAccountBalance();
            logEntry.setBalanceAmount(
                    (bankAccountBalance != null && !bankAccountBalance.trim().isEmpty())
                            ? Double.valueOf(bankAccountBalance)
                            : null
            );

            logEntry.setBankName(
                    vendorResp.getData().getBankName() != null
                            ? vendorResp.getData().getBankName()
                            : null
            );

            logEntry.setTransactionStatus(
                    vendorResp.getStatuscode() != null
                            ? vendorResp.getStatuscode()
                            : null
            );

            logEntry.setStatusMessage(
                    vendorResp.getStatus() != null
                            ? vendorResp.getStatus()
                            : null
            );

            logEntry.setAadhaarNumber(
                    req.getAadhaarNumber() != null
                            ? req.getAadhaarNumber()
                            : null
            );

            logEntry.setMobileNumber(
                    req.getMobile() != null
                            ? req.getMobile()
                            : null
            );

            logEntry.setRequestPayload(
                    requestJson != null
                            ? requestJson
                            : null
            );

            logEntry.setResponsePayload(
                    responseJson != null
                            ? responseJson
                            : null
            );

            log.info("Saving AEPS Balance Enquiry Log");

            aepsBalanceEnquiryRepository.save(logEntry);

            log.info("AEPS Balance Enquiry Log Saved Successfully");

//            }

            log.info(
                    "Fetching Scheme Rate | SchemeCode={}, TransactionType={}, Amount={}",
                    req.getSchemeCode(),
                    req.getTransactionType(),
                    req.getAmount()
            );

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> {

                        log.error(
                                "No Scheme Rate Found | SchemeCode={}, TransactionType={}, Amount={}",
                                req.getSchemeCode(),
                                req.getTransactionType(),
                                req.getAmount()
                        );

                        return new RuntimeException(
                                "No matching scheme rate found for amount: " + req.getAmount()
                        );
                    });

            log.info("Scheme Rate Found Successfully");
            log.info("SchemeId={}", selectedScheme.getId());
            log.info("OperatorCode={}", selectedScheme.getOperatorCode());
            log.info("SchemeCode={}", selectedScheme.getSchemeCode());
            log.info("OperatorName={}", req.getTransactionType());
            log.info("Amount={}", req.getAmount());

            // Build DTO for transaction engine
            log.info("Building Transaction DTO");

            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();  //BE

            dto.setBaseTranxId(baseTxnId);
//            dto.setAmount(req.getTransactionAmount().doubleValue());
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName("BE");
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            Products product = subProducts.getProduct();
//            dto.setTransactionType(product.getTransactionType());
            dto.setTransactionType("Credit");
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());
            dto.setLatitude(Double.valueOf(latitude));
            log.info("Latitude={}", dto.getLatitude());
            dto.setLongitude(Double.valueOf(longitude));
            log.info("Mapping Vendor Status");

//            String vendorStatus = InstantpayResponseMapper.map(
//                    vendorResp.getStatuscode(),
//                    vendorResp.getOrderid()
//            );

            log.info("VendorStatus={}", vendorStatus);

            log.info(
                    "Transaction DTO Prepared | BaseTxnId={}, Amount={}, ConfirmAmount={}, OperatorName={}, OperatorCode={}, TransactionType={}, WalletType={}, ProductCode={}, SubProductCode={}, HierarchyId={}, Latitude={}, Longitude={}",
                    dto.getBaseTranxId(),
                    dto.getAmount(),
                    dto.getConfirmAmount(),
                    dto.getOperatorName(),
                    dto.getOperatorCode(),
                    dto.getTransactionType(),
                    dto.getWalletType(),
                    dto.getProductCode(),
                    dto.getSubProductCode(),
                    dto.getHierarchyId(),
                    dto.getLatitude(),
                    dto.getLongitude()
            );

            log.info(
                    "Scheme Details | SchemeId={}, OperatorCode={}",
                    selectedScheme.getId(),
                    selectedScheme.getOperatorCode()
            );

            log.info("Calling Transaction Engine");

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            log.info("Transaction Engine Completed Successfully");

            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                log.info("Final Status = SUCCESS");
                return new CommonResponse<>(200, "Transaction Successful", vendorResp);
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                log.info("Final Status = PENDING");
                return new CommonResponse<>(202, "Transaction Pending", vendorResp);
            }
            else {
                log.info("Final Status = FAILED");
                return new CommonResponse<>(400, "Transaction Failed", vendorResp);
            }

        } catch (Exception e) {
            log.error("Balance Enquiry Exception Occurred");
            log.error("Exception Message={}", e.getMessage());
            log.error("Stack Trace", e);

            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }
    }


    @Override
    public CommonResponse<InstantpayMiniStatementResponseDto> miniStatement(
            InstantpayMiniStatementRequestDto req,Double latitude,Double longitude) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            String orderId = baseTransactionId;
            log.info("[AEPS BE] Start | orderId={}", orderId);

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;


            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String hardCodeInput = "MS";

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));


            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "MS"
            );

            String baseUrl = credentials.getBaseUrlProd();
            String saltKey = credentials.getSaltKey();

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));


//            String outletId = merchant.getMerchantLoginId();
            String outletId = merchant.getOutletId();

            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            req.getAadhaarNumber(),
                            saltKey);

            String externalRef =
                    "AEPS-MINI-" + req.getRetailerId() + "-" + System.currentTimeMillis();

            String url = baseUrl + "/fi/aeps/miniStatement";

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("bankiin", req.getBankiin());
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("mobile", req.getMobile());
            body.put("externalRef", externalRef);
            body.put("captureType", "FINGER");

            Map<String, Object> bio = new LinkedHashMap<>();

            bio.put("encryptedAadhaar", encryptedAadhaar);
            bio.put("dc", req.getBiometricData().getDc());
            bio.put("ci", req.getBiometricData().getCi());
            bio.put("hmac", req.getBiometricData().getHmac());
            bio.put("dpId", req.getBiometricData().getDpID());
            bio.put("mc", req.getBiometricData().getMc());
            bio.put("pidDataType", req.getBiometricData().getPidDatatype());
            bio.put("sessionKey", req.getBiometricData().getSessionKey());
            bio.put("mi", req.getBiometricData().getMi());
            bio.put("rdsId", req.getBiometricData().getRdsID());
            bio.put("errCode", req.getBiometricData().getErrCode());
            bio.put("errInfo", req.getBiometricData().getErrInfo());
            bio.put("fCount", req.getBiometricData().getFCount());
            bio.put("fType", req.getBiometricData().getFType());
            bio.put("iCount", req.getBiometricData().getICount());
            bio.put("iType", req.getBiometricData().getIType());
            bio.put("pCount", req.getBiometricData().getPCount());
            bio.put("pType", req.getBiometricData().getPType());
            bio.put("srno", req.getBiometricData().getSrno());
            bio.put("sysid", req.getBiometricData().getSysid());
            bio.put("ts", req.getBiometricData().getTs());
            bio.put("pidData", req.getBiometricData().getPiddata());
            bio.put("qScore", req.getBiometricData().getQScore());
            bio.put("nmPoints", req.getBiometricData().getNmPoints());
            bio.put("rdsVer", req.getBiometricData().getRdsVer());

            body.put("biometricData", bio);

            ObjectMapper mapper = new ObjectMapper();
            requestJson = mapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<InstantpayMiniStatementResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayMiniStatementResponseDto.class
                    );

            InstantpayMiniStatementResponseDto vendorResp = response.getBody();

            responseJson = mapper.writeValueAsString(vendorResp);

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (vendorResp == null) {
                return new CommonResponse<>(500, "Null vendor response", null);
            }

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

            String txnId;
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("TXN", baseTxnId);
            } else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                txnId = buildTransactionId("PND", baseTxnId);
            } else {
                txnId = buildTransactionId("FLD", baseTxnId);
            }

            // ===== SAVE MINI STATEMENT LOG =====
//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())
//                    && vendorResp.getData() != null) {
            AepsMiniStatement log = new AepsMiniStatement();
            log.setRetailerId(
                    req.getRetailerId() != null ? req.getRetailerId() : null
            );

            log.setVendorId(
                    vendor.getId() != null ? vendor.getId() : null
            );

            log.setTransactionId(
                    txnId != null ? txnId : null
            );

            log.setAadhaarNumber(
                    req.getAadhaarNumber() != null ? req.getAadhaarNumber() : null
            );

            log.setMobileNumber(
                    req.getMobile() != null ? req.getMobile() : null
            );

            log.setBankName(
                    vendorResp.getData().getBankName() != null
                            ? vendorResp.getData().getBankName()
                            : null
            );

            String bankAccountBalance = vendorResp.getData().getBankAccountBalance();
            log.setBalanceAmount(
                    (bankAccountBalance != null && !bankAccountBalance.trim().isEmpty())
                            ? Double.valueOf(bankAccountBalance)
                            : null
            );

            String transactionValue = vendorResp.getData().getTransactionValue();
            log.setTransactionAmount(
                    (transactionValue != null && !transactionValue.trim().isEmpty())
                            ? Double.valueOf(transactionValue)
                            : null
            );

            log.setStatusMessage(
                    vendorResp.getStatus() != null
                            ? vendorResp.getStatus()
                            : null
            );

            log.setRequestPayload(
                    requestJson != null
                            ? requestJson
                            : null
            );

            log.setResponsePayload(
                    responseJson != null
                            ? responseJson
                            : null
            );

            aepsMiniStatementRepository.save(log);
//            }

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();  //BE
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName("BE");
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);

            Products product = subProducts.getProduct();
//            dto.setTransactionType(product.getTransactionType());
            dto.setTransactionType("Credit");
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());
            dto.setLatitude(Double.valueOf(latitude));
            dto.setLongitude(Double.valueOf(longitude));

//            String vendorStatus = InstantpayResponseMapper.map(
//                    vendorResp.getStatuscode(),
//                    vendorResp.getOrderid()
//            );

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(200, "Transaction Successful", vendorResp);
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", vendorResp);
            }
            else {
                return new CommonResponse<>(400, "Transaction Failed", vendorResp);
            }

        } catch (Exception e) {

            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }
    }

    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }
}

