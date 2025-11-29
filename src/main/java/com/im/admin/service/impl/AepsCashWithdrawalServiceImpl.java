package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.AepsCashWithdrawalLog;
import com.im.admin.repositories.AepsCashWithdrawalRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.service.AepsCashWithdrawalService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.util.FingpayEncryptionUtil;

@Service
public class AepsCashWithdrawalServiceImpl implements AepsCashWithdrawalService {

    private static final String FINGPAY_CW_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/cashWithdrawal/merchant/withdrawal";

    private static final String MERCHANT_MD5_PIN = "81dc9bdb52d04dc20036dbd8313ed055";
    private static final String SUPER_USERNAME = "nftpayd";
    private static final int SUPER_MERCHANT_ID = 1396;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AepsCashWithdrawalRepository cashWithdrawalRepository;

    @Override
    public ResponseEntity<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req) {

        try {
            // Load vendor
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            // Load retailer
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // Set backend values
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantUserName(SUPER_USERNAME);
            req.setMerchantPin(MERCHANT_MD5_PIN);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));

            req.setTransactionType("CW");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTranId("CW-" + System.currentTimeMillis());

            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());
            req.setPaymentType("B");
            req.setLanguageCode("en");

            // UID Details
//            AepsCashWithdrawalRequest.P2CardnumberORUID uid = new AepsCashWithdrawalRequest.P2CardnumberORUID();
//            uid.setAdhaarNumber(req.getCardnumberORUID().getAdhaarNumber());
//            uid.setNationalBankIdentificationNumber(req.getCardnumberORUID().getNationalBankIdentificationNumber());
//            uid.setIndicatorforUID(0);
//            req.setCardnumberORUID(uid);

            AepsCashWithdrawalRequest.P2CardnumberORUID uid = new AepsCashWithdrawalRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setIndicatorforUID(0); // 0 for Aadhaar
            uid.setNationalBankIdentificationNumber(req.getCardnumberORUID() != null ?
                    req.getCardnumberORUID().getNationalBankIdentificationNumber() : null);

            req.setCardnumberORUID(uid);

            // Convert to JSON
            String plainJson = objectMapper.writeValueAsString(req);

            // Load Fingpay public certificate
            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            // 🔥 Call your encryption utility
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", req.getDeviceIMEI() != null ? req.getDeviceIMEI() : "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            // 🔥 CALL FINGPAY API
            ResponseEntity<AepsCashWithdrawalResponse> response = restTemplate.exchange(
                    FINGPAY_CW_URL,
                    HttpMethod.POST,
                    entity,
                    AepsCashWithdrawalResponse.class
            );

            AepsCashWithdrawalResponse body = response.getBody();

            // Save Successful Transaction
            if (body != null && body.isStatus() && body.getData() != null) {

                if ("00".equals(body.getData().getResponseCode())) {

                    AepsCashWithdrawalLog log = new AepsCashWithdrawalLog();
                    log.setBankRrn(body.getData().getBankRRN());
                    log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
                    log.setMerchantTxnId(body.getData().getMerchantTxnId());
                    log.setTransactionAmount(body.getData().getTransactionAmount());
                    log.setTransactionStatus(body.getData().getTransactionStatus());
                    log.setRequestPayload(plainJson);
                    log.setResponsePayload(objectMapper.writeValueAsString(body));
                    log.setRetailerId(req.getRetailerId());
                    log.setVendorId(vendor.getId());

                    cashWithdrawalRepository.save(log);
                }
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            AepsCashWithdrawalResponse dto = new AepsCashWithdrawalResponse();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500L);
            return ResponseEntity.ok(dto);
        }
    }
    }

