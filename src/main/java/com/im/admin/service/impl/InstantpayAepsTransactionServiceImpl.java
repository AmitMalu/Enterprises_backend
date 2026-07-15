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


            String txnId = buildTransactionId("TXN", baseTxnId);

            // ===============================
            // SAVE TRANSACTION LOG
            // ===============================

//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())) {

                AepsCashWithdrawalLog logEntry = new AepsCashWithdrawalLog();

                logEntry.setRetailerId(req.getRetailerId());
                logEntry.setVendorId(vendor.getId());
                logEntry.setMerchantTxnId(externalRef);
//                logEntry.setTransactionId(vendorResp.getOrderid());
                logEntry.setTransactionId(txnId);
                logEntry.setTransactionAmount(
                        Double.valueOf(vendorResp.getData().getTransactionValue())
                );
                logEntry.setBalanceAmount(
                        Double.valueOf(vendorResp.getData().getClosingBalance())
                );
                logEntry.setBankName(vendorResp.getData().getBankName());
                logEntry.setTransactionStatus(vendorResp.getStatuscode());
                logEntry.setStatusMessage(vendorResp.getStatus());
                logEntry.setRequestPayload(requestJson);
                logEntry.setResponsePayload(mapper.writeValueAsString(vendorResp));

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

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

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
    public CommonResponse<InstantpayBalanceEnquiryResponseDto> balanceEnquiry(InstantpayBalanceEnquiryRequestDto req,Double latitude,Double longitude) {

        try {
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            log.info("Fetching SubProduct for id={}", req.getSubProductId());

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            String orderId = baseTransactionId;
            log.info("[AEPS BE] Start | orderId={}", orderId);

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            LocalDateTime requestTime = LocalDateTime.now();
            String requestJson = null;
            String responseJson = null;

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

            String hardCodeInput = "BE";

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));

            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "BE"
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
                    "AEPS-BAL-" + req.getRetailerId() + "-" + System.currentTimeMillis();

            String url = baseUrl + "/fi/aeps/balanceInquiry";

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

            ResponseEntity<InstantpayBalanceEnquiryResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayBalanceEnquiryResponseDto.class
                    );

            InstantpayBalanceEnquiryResponseDto vendorResp = response.getBody();

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

                return new CommonResponse<>(
                        500,
                        "Vendor returned empty response",
                        null
                );
            }

            String txnId = buildTransactionId("TXN", baseTxnId);


            // ===== SAVE AEPS BALANCE LOG =====
//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())
//                    && vendorResp.getData() != null) {

                AepsBalanceEnquiry logEntry = new AepsBalanceEnquiry();

                logEntry.setRetailerId(req.getRetailerId());
                logEntry.setVendorId(vendor.getId());
                logEntry.setMerchantTxnId(externalRef);
//                logEntry.setTransactionId(vendorResp.getOrderid());
                logEntry.setTransactionId(txnId);

                logEntry.setBalanceAmount(
                        Double.valueOf(vendorResp.getData().getBankAccountBalance())
                );

                logEntry.setBankName(vendorResp.getData().getBankName());
                logEntry.setTransactionStatus(vendorResp.getStatuscode());
                logEntry.setStatusMessage(vendorResp.getStatus());

                logEntry.setAadhaarNumber(req.getAadhaarNumber());
                logEntry.setMobileNumber(req.getMobile());

                logEntry.setRequestPayload(requestJson);
                logEntry.setResponsePayload(responseJson);

                aepsBalanceEnquiryRepository.save(logEntry);

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

            log.info(
                    "Scheme Rate Found | OperatorCode={}",
                    selectedScheme.getOperatorCode()
            );

            // Build DTO for transaction engine
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
            dto.setLongitude(Double.valueOf(longitude));

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

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

            String txnId = buildTransactionId("TXN", baseTxnId);

            // ===== SAVE MINI STATEMENT LOG =====
//            if ("TXN".equalsIgnoreCase(vendorResp.getStatuscode())
//                    && vendorResp.getData() != null) {

                AepsMiniStatement log = new AepsMiniStatement();

                log.setRetailerId(req.getRetailerId());
                log.setVendorId(vendor.getId());
                log.setTransactionId(txnId);

                log.setAadhaarNumber(req.getAadhaarNumber());
                log.setMobileNumber(req.getMobile());

                log.setBankName(vendorResp.getData().getBankName());
                log.setBalanceAmount(Double.valueOf(vendorResp.getData().getBankAccountBalance()));

                log.setTransactionAmount(Double.valueOf(vendorResp.getData().getTransactionValue()));
                log.setStatusMessage(vendorResp.getStatus());

                log.setRequestPayload(requestJson);
                log.setResponsePayload(responseJson);

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

            String vendorStatus = InstantpayResponseMapper.map(
                    vendorResp.getStatuscode(),
                    vendorResp.getOrderid()
            );

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

