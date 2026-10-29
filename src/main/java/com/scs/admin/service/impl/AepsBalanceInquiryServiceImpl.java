package com.scs.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.dto.TransactionSchemeDetailsRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.entities.enums.AepsResponseMapper;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.entities.product.Products;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.*;
import com.scs.admin.request.AepsBalanceInquiryRequest;
import com.scs.admin.request.AepsMiniStatementRequest;
import com.scs.admin.response.AepsBalanceInquiryResponse;
import com.scs.admin.response.AepsCashWithdrawalResponse;
import com.scs.admin.response.AepsMiniStatementResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AepsBalanceInquiryService;
import com.scs.admin.service.AepsVendorService;
import com.scs.admin.util.AepsVendorFactory;
import com.scs.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;
import com.scs.admin.util.FingpayEncryptionUtil;

import javax.annotation.processing.SupportedAnnotationTypes;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Service
public class AepsBalanceInquiryServiceImpl implements AepsBalanceInquiryService {
    @Autowired
    private OperatorsRepository operatorsRepository;

    private static final String FINGPAY_BALANCE_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/balanceInquiry/merchant/getBalance";

    private static final String MERCHANT_MD5_PIN = "796c3ee556ac31f3754a38cfd15b8044";  //1234d
    /** Fingpay super-merchant username: set aeps.fingpay.super-username to your own Fingpay account. */
    @org.springframework.beans.factory.annotation.Value("${aeps.fingpay.super-username:}")
    private String SUPER_USERNAME;
    private static final int SUPER_MERCHANT_ID = 1396; // Replace with your actual super merchant ID


    @Autowired private CommonMethods commonMethods;
    @Autowired private SubProductsRepository subProductsRepository;
    @Autowired private RetailerRepository retailerRepository;
    @Autowired private VendorRoutingRepository vendorRoutingRepository;
    @Autowired private VendorCredentialsRepository vendorCredentialsRepository;
    @Autowired private AepsVendorFactory aepsVendorFactory;
    @Autowired private AepsCashWithdrawalRepository cashWithdrawalRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineService;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

  @Autowired
  private AepsBalanceEnquiryRepository aepsBalanceEnquiryRepository;

  @Autowired
  private  AepsMiniStatementRepository aepsMiniStatementRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public CommonResponse<AepsBalanceInquiryResponse>  balanceInquiry(AepsBalanceInquiryRequest req, @RequestHeader Double latitude, @RequestHeader Double longitude) {



        String baseTransactionId = commonMethods.generateBaseTransactionId();
        String orderId = "AEPS" + baseTransactionId;
        log.info("[AEPS BE] Start | orderId={}", orderId);

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // ===== Validate SubProduct =====
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

            // ===== Load Vendor Credentials =====
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            vendorId=vendor.getId();

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials missing"));

            // ===== Retailer =====
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            String hardCodeInput = "BE";

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));

            // ==== Backend controlled fields ====
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(retailer.getRetailerId());
            req.setMerchantUserName(retailer.getRetailerId());
            req.setMerchantPin(MERCHANT_MD5_PIN);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String timestamp = sdf.format(new Date());
            req.setRequestRemarks("Test");
            req.setTimestamp(timestamp);
//            req.setTransactionType("BALANCE ENQUIRY");
            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "BE"
            );
            req.setServiceType("AEPS");
            req.setMerchantTransactionId("BE-" + System.currentTimeMillis());

            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
//            req.setMobileNumber(retailer.getMobileNumber());
            req.setMobileNumber(req.getMobileNumber());

            req.setPaymentType("B");
            req.setLanguageCode("en");

            // Aadhaar
            AepsBalanceInquiryRequest.P2CardnumberORUID uid = new AepsBalanceInquiryRequest.P2CardnumberORUID();
            uid.setIndicatorforUID(0);
//            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setAdhaarNumber(req.getAdhaarNumber());
//            uid.setVirtualId("");
            uid.setNationalBankIdentificationNumber(req.getNationalBankIdentificationNumber());

            req.setCardnumberORUID(uid);

            // Request JSON
            String plainJson = objectMapper.writeValueAsString(req);

            requestJson = plainJson;


            // ===== Vendor Call =====
            AepsVendorService vendorService = aepsVendorFactory.getVendor(vendor.getVendorName());

            if (vendorService == null) {
                return new CommonResponse<>(500, "Vendor not implemented", null);
            }


            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            // vemndor call
            ResponseEntity<AepsBalanceInquiryResponse> vendorResp =
                    vendorService.processBalanceInquiry(req, credentials);

            AepsBalanceInquiryResponse response = vendorResp.getBody();

            responseJson = objectMapper.writeValueAsString(response);

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );


            if (response == null) {
                return new CommonResponse<>(500, "Null vendor response", null);

            }

            String txnId = buildTransactionId("TXN", baseTxnId);

            // ===== SAVE SUCCESS LOGS =====
            if (response.isStatus()
                    && response.getData() != null
                    && "00".equals(response.getData().getResponseCode())
                    && response.getData().getBankRRN() != null) {

                AepsBalanceEnquiry log = new AepsBalanceEnquiry();
                log.setRetailerId(req.getRetailerId());
                log.setVendorId(vendor.getId());
                log.setFingpayTransactionId(response.getData().getFingpayTransactionId());
                log.setBankRrn(response.getData().getBankRRN());
                log.setMerchantTxnId(response.getData().getMerchantTxnId());
                log.setTerminalId(response.getData().getTerminalId());
                log.setTransactionStatus(response.getData().getTransactionStatus());
                log.setBalanceAmount(response.getData().getBalanceAmount());
                log.setRequestPayload(plainJson);
                log.setResponsePayload(objectMapper.writeValueAsString(response));
                log.setTransactionId(txnId);


                //

                log.setAadhaarNumber(req.getCardnumberORUID().getAdhaarNumber());
                log.setMobileNumber(req.getMobileNumber());
//                log.setBankName(response.getData().getBankName());
                log.setTransactionAmount(response.getData().getTransactionAmount());
                log.setStatusMessage(response.getData().getErrorMessage()); // or correct field


                aepsBalanceEnquiryRepository.save(log);
            }

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getTransactionAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getTransactionAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();  //BE
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getTransactionAmount().doubleValue());
            dto.setConfirmAmount(req.getTransactionAmount().doubleValue());
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


            // ===== MAPPING VENDOR STATUS =====
            AepsBalanceInquiryResponse.BalanceData data = response.getData();

            // Handle null data safely
            String responseCode = (data != null) ? data.getResponseCode() : null;
            String bankRRN = (data != null) ? data.getBankRRN() : null;
//
//            String vendorStatus = AepsResponseMapper.map(
//                    response.isStatus(),
//                    data.getResponseCode(),
//                    data.getBankRRN()
//            );

            String vendorStatus = AepsResponseMapper.map(
                    response.isStatus(),
                    responseCode,
                    bankRRN
            );

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

//            String txnId = buildTransactionId("TXN", baseTxnId);

            if (response != null && response.getData() != null) {
                response.getData().setTransactionId(txnId);
            }


            // ===== FINAL RESPONSE =====
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return CommonResponse.success(response, "Transaction Successful");
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", response);
            }
            else {
                return new CommonResponse<>(400, "Transaction Failed", response);
            }

        } catch (Exception e) {

            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }


    }

    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }


    @Override
    public CommonResponse<AepsMiniStatementResponse> ministatement(AepsMiniStatementRequest req, @RequestHeader Double latitude, @RequestHeader Double longitude) {

        String baseTransactionId = commonMethods.generateBaseTransactionId();
        String orderId = "AEPS" + baseTransactionId;
        log.info("[AEPS BE] Start | orderId={}", orderId);

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // ===== Validate SubProduct =====
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

            // ===== Load Vendor Credentials =====
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            vendorId=vendor.getId();

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials missing"));

            // ===== Retailer =====
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            String hardCodeInput = "MS";

            Operators operator = operatorsRepository
                    .findByHardCode(hardCodeInput)
                    .orElseThrow(() -> new RuntimeException("Operator not found: " + hardCodeInput));


            // ==== Backend controlled fields ====
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(retailer.getRetailerId());
            req.setMerchantUserName(retailer.getRetailerId());
            req.setMerchantPin(MERCHANT_MD5_PIN);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String timestamp = sdf.format(new Date());

            req.setRequestRemarks("Test");
            req.setTimestamp(timestamp);
//            req.setTransactionType("MS");
            req.setTransactionType(
                    operator.getOperatorName() != null
                            ? operator.getOperatorName()
                            : "MS"
            );
            req.setServiceType("AEPS");
            req.setMerchantTranId("MS-" + System.currentTimeMillis());

            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
//            req.setMobileNumber(retailer.getMobileNumber());
            req.setMobileNumber(req.getMobileNumber());
            req.setPaymentType("B");
            req.setLanguageCode("en");

            // Aadhaar
            AepsMiniStatementRequest.P2CardnumberORUID uid = new AepsMiniStatementRequest.P2CardnumberORUID();
            uid.setIndicatorforUID(0);
//            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setAdhaarNumber(req.getAdhaarNumber());
//            uid.setVirtualId("");
            uid.setNationalBankIdentificationNumber(req.getNationalBankIdentificationNumber());
            req.setCardnumberORUID(uid);

            // Request JSON
            String plainJson = objectMapper.writeValueAsString(req);

            requestJson = plainJson;


            // ===== Vendor Call =====
            AepsVendorService vendorService = aepsVendorFactory.getVendor(vendor.getVendorName());

            if (vendorService == null) {
                return new CommonResponse<>(500, "Vendor not implemented", null);
            }


            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;


            // vemndor call
            ResponseEntity<AepsMiniStatementResponse> vendorResp =
                    vendorService.processMiniStateMent(req, credentials);

            AepsMiniStatementResponse response = vendorResp.getBody();

            responseJson = objectMapper.writeValueAsString(response);

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );


            if (response == null) {
                return new CommonResponse<>(500, "Null vendor response", null);

            }

            String txnId = buildTransactionId("TXN", baseTxnId);


            // ===== SAVE SUCCESS LOGS =====
            if (response.isStatus()
                    && response.getData() != null
                    && "00".equals(response.getData().getResponseCode())
                    && response.getData().getBankRRN() != null) {

                AepsMiniStatement log = new AepsMiniStatement();
                log.setRetailerId(req.getRetailerId());
                log.setVendorId(vendor.getId());
                log.setFingpayTransactionId(response.getData().getFingpayTransactionId());
                log.setBankRrn(response.getData().getBankRRN());
                log.setMerchantTxnId(response.getData().getMerchantTxnId());
                log.setTerminalId(response.getData().getTerminalId());
                log.setTransactionStatus(response.getData().getTransactionStatus());
                log.setBalanceAmount(response.getData().getBalanceAmount());
                log.setRequestPayload(plainJson);
                log.setResponsePayload(objectMapper.writeValueAsString(response));
                log.setTransactionId(txnId);

                //

                log.setAadhaarNumber(req.getCardnumberORUID().getAdhaarNumber());
                log.setMobileNumber(req.getMobileNumber());
//                log.setBankName(response.getData().getBankName());
                log.setTransactionAmount(response.getData().getTransactionAmount());
                log.setStatusMessage(response.getData().getErrorMessage()); // or correct field


                aepsMiniStatementRepository.save(log);
            }

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getTransactionAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getTransactionAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();  //MS
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getTransactionAmount().doubleValue());
            dto.setConfirmAmount(req.getTransactionAmount().doubleValue());
            dto.setOperatorName("MS");
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


            // ===== MAPPING VENDOR STATUS =====
            AepsBalanceInquiryResponse.BalanceData data = response.getData();

            String vendorStatus = AepsResponseMapper.map(
                    response.isStatus(),
                    data.getResponseCode(),
                    data.getBankRRN()
            );

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

//            String txnId = buildTransactionId("TXN", baseTxnId);

            if (response != null && response.getData() != null) {
                response.getData().setTransactionId(txnId);
            }


            // ===== FINAL RESPONSE =====
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return CommonResponse.success(response, "Transaction Successful");
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", response);
            }
            else {
                return new CommonResponse<>(400, "Transaction Failed", response);
            }

        } catch (Exception e) {

            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }
    }


}

