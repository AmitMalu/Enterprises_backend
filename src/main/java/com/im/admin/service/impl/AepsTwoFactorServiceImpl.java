package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.Aeps2FACheckStatusRequest;
import com.im.admin.entities.AepsTwoFa;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.Aeps2FaRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.response.Aeps2FaResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsTwoFactorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.FingpayEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
public class AepsTwoFactorServiceImpl implements AepsTwoFactorService {

    private static final String MERCHANT_MD5_PIN = "81dc9bdb52d04dc20036dbd8313ed055";  //1234d
    private static final String SUPER_USERNAME = "nftpayd";
    private static final String FINGPAY_2FA_URL = "https://fingpayap.tapits.in/fpaepsservice/auth/tfauth/merchant/validate/aadhar";
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
    private Aeps2FaRepository aAeps2FaRepository;

    @Override
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req) {

        // ====== STEP 1: Load Vendor Credentials ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        CommonResponse<Aeps2FaResponseDto> common = new CommonResponse<>();

        try {
            // Load retailer
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // Backend fields
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            req.setMerchantUserName(SUPER_USERNAME);
            req.setMerchantPin(MERCHANT_MD5_PIN);
            req.setTransactionType("AUO");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTranId("TXN" + System.currentTimeMillis());
            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());

            // UID block
            Aeps2FaRequest.P2CardnumberORUID uid = new Aeps2FaRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(retailer.getAadhaarNumber());
            uid.setIndicatorforUID(0);
            uid.setNationalBankIdentificationNumber(null);
            uid.setVirtualId(null);
            req.setCardnumberORUID(uid);

            // Convert to JSON
            String plainJson = objectMapper.writeValueAsString(req);

            // Load Fingpay Public Key
            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            // Encrypt
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            // Vendor API Call
            ResponseEntity<Aeps2FaResponseDto> vendorResp = restTemplate.exchange(
                    FINGPAY_2FA_URL,
                    HttpMethod.POST,
                    entity,
                    Aeps2FaResponseDto.class
            );

            Aeps2FaResponseDto body = vendorResp.getBody();

            // ========== Save Log if Success ==========
            if (body != null && body.isStatus() && body.getData() != null) {
                if ("00".equals(body.getData().getResponseCode())) {
                    AepsTwoFa log = new AepsTwoFa();
                    log.setTefPkId(body.getData().getTefPkId());
                    log.setStan(body.getData().getStan());
                    log.setBankRrn(body.getData().getBankRrn());
                    log.setFpRrn(body.getData().getFpRrn());
                    log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
                    log.setMerchantTranId(body.getData().getMerchantTranId());
                    log.setResponseCode(body.getData().getResponseCode());
                    log.setResponseMessage(body.getData().getResponseMessage());
                    log.setMobileNumber(body.getData().getMobileNumber());
                    log.setTransactionTimestamp(body.getData().getTransactionTimestamp());
                    log.setRetailerId(req.getRetailerId());
                    log.setVendor(vendor);

                    aAeps2FaRepository.save(log);
                }
            }

            // ========= Success CommonResponse =========
            common.setStatusCode(vendorResp.getStatusCodeValue());
            common.setMessage("Success");
            common.setData(body);

            return ResponseEntity.status(vendorResp.getStatusCode()).body(common);

        } catch (Exception e) {
            e.printStackTrace();

            // ========= Error CommonResponse =========
            common.setStatusCode(500);
            common.setMessage("Vendor API failed: " + e.getMessage());
            common.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(common);
        }
    }


    @Override
    public ResponseEntity<CommonResponse<AepsTwoFa>> check2FAStatus(Aeps2FACheckStatusRequest aeps2FACheckStatusRequest) {
        CommonResponse<AepsTwoFa> response = new CommonResponse<>();

        // fetch latest 2FA record
        Optional<AepsTwoFa> optional =
                aAeps2FaRepository.findTopByRetailerIdOrderByIdDesc(aeps2FACheckStatusRequest.getRetailerId());

        // 1️⃣ No record found → 2FA never done
        if (optional.isEmpty()) {
            log.warn("2FA Check: No record found for retailerId {}", aeps2FACheckStatusRequest.getRetailerId());

            response.setStatusCode(400);
            response.setMessage("Aadhaar not validated. Please complete 2FA first.");
            response.setData(null);
            return ResponseEntity.ok(response);
        }

        AepsTwoFa logData = optional.get();

        // 2️⃣ Check responseCode
        if (!"00".equals(logData.getResponseCode())) {
            log.warn("2FA FAILED for retailer {} | Response Code {}",
                    aeps2FACheckStatusRequest.getRetailerId(), logData.getResponseCode());

            response.setStatusCode(400);
            response.setMessage("2FA attempt failed. Please try again.");
            response.setData(logData);
            return ResponseEntity.ok(response);
        }

        // 3⃣ EXPIRY CHECK — 24 HOURS
        long now = System.currentTimeMillis();
        long expiryTime = logData.getCreatedAt() + (24 * 60 * 60 * 1000); // 24 hours

        if (now > expiryTime) {
            log.warn("2FA EXPIRED for retailer {} | CreatedAt {}",
                    aeps2FACheckStatusRequest.getRetailerId(), logData.getCreatedAt());

            response.setStatusCode(400);
            response.setMessage("2FA expired. Please do 2FA again.");
            response.setData(logData);
            return ResponseEntity.ok(response);
        }

        // 4⃣ SUCCESS: 2FA is valid
        log.info("2FA VALID for retailer {} | TranId {}",
                aeps2FACheckStatusRequest.getRetailerId(), logData.getFingpayTransactionId());

        response.setStatusCode(200);
        response.setMessage("2FA is valid.");
        response.setData(logData);
        return ResponseEntity.ok(response);
    }
    }


