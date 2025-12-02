package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.AepsResponseMapper;
import com.im.admin.entities.enums.DmtResponseMapper;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsCashWithdrawalService;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.AepsVendorFactory;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class AepsCashWithdrawalServiceImpl implements AepsCashWithdrawalService {

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
    private TransactionEngineServiceImpl transactionEngineServiceImpl;


    private static final String SUPER_USERNAME = "nftpayd";
    private static final String MERCHANT_MD5_PIN = "81dc9bdb52d04dc20036dbd8313ed055";
    private static final int SUPER_MERCHANT_ID = 1396;

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req) {

        String orderId = "AEPS" + commonMethods.generateBaseTransactionId();
        log.info("[AEPS CW] Start | orderId={}", orderId);

        try {
            // Validate SubProduct
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

            // Check retailer balance
            BigDecimal availableBalance = commonMethods.getCurrentBalance(req.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(req.getTransactionAmount()) < 0) {
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            // Vendor loading
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials =
                    vendorCredentialsRepository.findByVendorIdAndProductId(
                            vendor.getId(), vendor.getProduct().getId()
                    ).orElseThrow(() -> new RuntimeException("Vendor credentials missing"));

            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // ==== Set Backend Values ====
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantUserName(SUPER_USERNAME);
            req.setMerchantPin(MERCHANT_MD5_PIN);
            req.setMerchantLoginId(retailer.getRetailerId());
            req.setTransactionType("CW");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTranId("CW-" + System.currentTimeMillis());
            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());
            req.setPaymentType("B");
            req.setLanguageCode("en");

            // UID details
            AepsCashWithdrawalRequest.P2CardnumberORUID uid = new AepsCashWithdrawalRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setIndicatorforUID(0);
            uid.setNationalBankIdentificationNumber(
                    req.getCardnumberORUID() != null ?
                            req.getCardnumberORUID().getNationalBankIdentificationNumber() :
                            null
            );
            req.setCardnumberORUID(uid);

            // ==== Call Vendor ====
            AepsVendorService vendorService = aepsVendorFactory.getVendor(vendor.getVendorName());

            if (vendorService == null) {
                return new CommonResponse<>(500, "Vendor not implemented", null);
            }

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            // vendor call
            ResponseEntity<AepsCashWithdrawalResponse> vendorResp =
                    vendorService.processCashWithdrawal(req, credentials);

            AepsCashWithdrawalResponse responseBody = vendorResp.getBody();


            // ==== Save logs if successful ====
            if (responseBody != null &&
                    responseBody.isStatus() &&
                    responseBody.getData() != null &&
                    "00".equals(responseBody.getData().getResponseCode())) {

                AepsCashWithdrawalLog logEntry = new AepsCashWithdrawalLog();
                logEntry.setBankRrn(responseBody.getData().getBankRRN());
                logEntry.setFingpayTransactionId(responseBody.getData().getFingpayTransactionId());
                logEntry.setMerchantTxnId(responseBody.getData().getMerchantTxnId());
                logEntry.setTransactionAmount(responseBody.getData().getTransactionAmount());
                logEntry.setTransactionStatus(responseBody.getData().getTransactionStatus());
                logEntry.setRequestPayload(objectMapper.writeValueAsString(req));
                logEntry.setResponsePayload(objectMapper.writeValueAsString(responseBody));
                logEntry.setRetailerId(req.getRetailerId());
                logEntry.setVendorId(vendor.getId());

                cashWithdrawalRepository.save(logEntry);
            }

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getTransactionType(),
                            req.getTransactionAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getTransactionAmount()));

            // Build DTO for transaction engine
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getTransactionAmount().doubleValue());
            dto.setConfirmAmount(req.getTransactionAmount().doubleValue());
            dto.setOperatorName(subProducts.getSubProductTitle());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);

            Products product = subProducts.getProduct();
            //dto.setTransactionType(product.getTransactionType());
            dto.setTransactionType("Credit");
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(req.getRetailerId());

//            AepsCashWithdrawalResponse responseBody = vendorResp.getBody();
            AepsCashWithdrawalResponse.CWData data = responseBody.getData();

            String vendorStatus = AepsResponseMapper.map(
                    responseBody.isStatus(),
                    data.getResponseCode(),
                    data.getBankRRN()
            );

            transactionEngineServiceImpl.processTransaction(dto, vendorStatus);

            // Wrap final API response
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)) {
                return CommonResponse.success(responseBody, "Transaction Successful");
            }
            else if ("PENDING".equalsIgnoreCase(vendorStatus)) {
                return new CommonResponse<>(202, "Transaction Pending", responseBody);
            }
            else {
                return new CommonResponse<>(400, "Transaction Failed", responseBody);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResponse<>(500, "Exception: " + e.getMessage(), null);
        }
    }
}

