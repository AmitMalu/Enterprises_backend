package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.AepsBalanceEnquiry;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.AepsBalanceEnquiryRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.service.AepsBalanceInquiryService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.im.admin.util.FingpayEncryptionUtil;

import javax.annotation.processing.SupportedAnnotationTypes;

@Service
public class AepsBalanceInquiryServiceImpl implements AepsBalanceInquiryService {

    private static final String FINGPAY_BALANCE_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/balanceInquiry/merchant/getBalance";

    private static final String MERCHANT_MD5_PIN = "81dc9bdb52d04dc20036dbd8313ed055";  //1234d
    private static final String SUPER_USERNAME = "nftpayd";
    private static final int SUPER_MERCHANT_ID = 1396; // Replace with your actual super merchant ID


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

  @Autowired
  private AepsBalanceEnquiryRepository aepsBalanceEnquiryRepository;


    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> balanceInquiry(AepsBalanceInquiryRequest req) {

        // STEP 1: Load Vendor Credentials Dynamically (same pattern as your reference)
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        try {
            // Load retailer
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // Backend injected fields
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            req.setMerchantUserName(SUPER_USERNAME);
            req.setMerchantPin(MERCHANT_MD5_PIN);

            // Balance enquiry specifics
            req.setTransactionType("BE");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTransactionId("BE-TXN-" + System.currentTimeMillis()); // merchantTxnId for BE

            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());
            req.setPaymentType("B");
            req.setLanguageCode("en");

            // UID block - use retailer's aadhaar by default
            AepsBalanceInquiryRequest.P2CardnumberORUID uid = new AepsBalanceInquiryRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setIndicatorforUID(0); // 0 for Aadhaar
            uid.setNationalBankIdentificationNumber(req.getCardnumberORUID() != null ?
                    req.getCardnumberORUID().getNationalBankIdentificationNumber() : null);
            uid.setVirtualId("");

            req.setCardnumberORUID(uid);

            // Convert to plain JSON
            String plainJson = objectMapper.writeValueAsString(req);

            // Load Fingpay Public Key certificate (same approach as your reference)
            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            // Use your encryption util to build payload
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

            // Make the external API call
            ResponseEntity<AepsBalanceInquiryResponse> response = restTemplate.exchange(
                    FINGPAY_BALANCE_URL,
                    HttpMethod.POST,
                    entity,
                    AepsBalanceInquiryResponse.class
            );

            AepsBalanceInquiryResponse body = response.getBody();

            // Save Log when final
            if (body != null && body.isStatus() && body.getData() != null) {
                // consider success only if responseCode == "00" and bankRRN present, per spec
                String responseCode = body.getData().getResponseCode();
                if ("00".equals(responseCode) && body.getData().getBankRRN() != null) {

                    AepsBalanceEnquiry log = new AepsBalanceEnquiry();

                    log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
                    log.setBankRrn(body.getData().getBankRRN());
                    log.setMerchantTxnId(body.getData().getMerchantTxnId());
                    log.setTerminalId(body.getData().getTerminalId());
                    log.setTransactionStatus(body.getData().getTransactionStatus());
                    log.setBalanceAmount(body.getData().getBalanceAmount());
                    log.setRequestPayload(plainJson);
                    log.setResponsePayload(objectMapper.writeValueAsString(body));
                    log.setRetailerId(req.getRetailerId());
                    log.setVendorId(vendor.getId());

                    aepsBalanceEnquiryRepository.save(log);
                }
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            AepsBalanceInquiryResponse dto = new AepsBalanceInquiryResponse();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500L);
            return ResponseEntity.ok(dto);
        }
    }
    }

