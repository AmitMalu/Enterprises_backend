package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
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
import com.im.admin.request.AepsTwoFaStatusDto;
import com.im.admin.response.Aeps2FaResponseDto;

import java.time.LocalDate;
import java.time.ZoneId;

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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class AepsTwoFactorServiceImpl implements AepsTwoFactorService {

    private static final String MERCHANT_MD5_PIN = "796c3ee556ac31f3754a38cfd15b8044";  //1234d
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

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req) {

        // ====== STEP 1: Load Vendor Credentials ======
        VendorRouting vendorRouting =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() ->
                        new RuntimeException("No credentials found for selected vendor"));

        CommonResponse<Aeps2FaResponseDto> common = new CommonResponse<>();

        // ===== Vendor Log Variables (outside try) =====
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId();

        try {
            // ====== STEP 2: Load Retailer ======
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // ====== STEP 3: Populate Backend Fields ======
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            req.setMerchantUserName(retailer.getRetailerId());
            req.setMerchantPin(MERCHANT_MD5_PIN);
            req.setTransactionType("AUO");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTranId("TXN" + System.currentTimeMillis());
            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());

            // ====== STEP 4: UID Block ======
            Aeps2FaRequest.P2CardnumberORUID uid =
                    new Aeps2FaRequest.P2CardnumberORUID();

            uid.setAdhaarNumber(req.getCardnumberORUID().getAdhaarNumber());
            uid.setIndicatorforUID("0");
            uid.setNationalBankIdentificationNumber("");

            req.setCardnumberORUID(uid);

            // ====== STEP 5: Convert Request to JSON ======
            String plainJson = objectMapper.writeValueAsString(req);
            requestJson = plainJson;

            // ====== STEP 6: Load Fingpay Public Key ======
            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream()
                            .readAllBytes()
            );

            // ====== STEP 7: Encrypt Request ======
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(
                            plainJson, certificate
                    );

            // ====== STEP 8: Prepare Headers ======
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity =
                    new HttpEntity<>(encrypted.body, headers);

            // ====== STEP 9: Vendor API Call ======
            ResponseEntity<Aeps2FaResponseDto> vendorResp =
                    restTemplate.exchange(
                            FINGPAY_2FA_URL,
                            HttpMethod.POST,
                            entity,
                            Aeps2FaResponseDto.class
                    );

            Aeps2FaResponseDto body = vendorResp.getBody();
            responseJson = objectMapper.writeValueAsString(body);

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // ====== STEP 10: Save Business Log ONLY on SUCCESS ======
            if (body != null && body.isStatus()
                    && body.getData() != null
                    && "00".equals(body.getData().getResponseCode())) {

//                AepsTwoFa log = new AepsTwoFa();
//                log.setTefPkId(body.getData().getTefPkId());
//                log.setStan(body.getData().getStan());
//                log.setBankRrn(body.getData().getBankRrn());
//                log.setFpRrn(body.getData().getFpRrn());
//                log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
//                log.setMerchantTranId(body.getData().getMerchantTranId());
//                log.setResponseCode(body.getData().getResponseCode());
//                log.setResponseMessage(body.getData().getResponseMessage());
//                log.setMobileNumber(body.getData().getMobileNumber());
//                log.setTransactionTimestamp(body.getData().getTransactionTimestamp());
//                log.setRetailerId(req.getRetailerId());
//                log.setCreatedAt(LocalDateTime.now());
//                log.setVendor(vendor);
//
//                aAeps2FaRepository.save(log);

                Optional<AepsTwoFa> existingOpt =
                        aAeps2FaRepository.findByRetailerId(req.getRetailerId());

                AepsTwoFa log;

                if (existingOpt.isPresent()) {
                    // UPDATE
                    log = existingOpt.get();

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
//                    log.setUpdatedAt(LocalDateTime.now());

                    aAeps2FaRepository.save(log);

                } else {
                    // INSERT
                    log = new AepsTwoFa();
                    log.setRetailerId(req.getRetailerId());
                    log.setVendor(vendor);
                    log.setCreatedAt(LocalDateTime.now());

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
//                    log.setUpdatedAt(LocalDateTime.now());

                    aAeps2FaRepository.save(log);

                }
            }

            // ====== STEP 11: FINAL RESPONSE ======
            if (body != null && body.isStatus()
                    && body.getData() != null
                    && "00".equals(body.getData().getResponseCode())) {

                common.setStatusCode(200);
                common.setMessage(body.getData().getResponseMessage());
                common.setData(body);
                return ResponseEntity.ok(common);

            } else {

                String vendorMsg = (body != null)
                        ? body.getMessage()
                        : "Vendor response empty";

                common.setStatusCode(400);
                common.setMessage(vendorMsg);
                common.setData(body);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(common);
            }

        } catch (Exception e) {

            common.setStatusCode(500);
            common.setMessage("Vendor API failed: " + e.getMessage());
            common.setData(null);

            try {
                responseJson = objectMapper.writeValueAsString(common);
            } catch (Exception ignored) {
                responseJson = e.getMessage();
            }

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(common);
        }
    }



    @Override
    public ResponseEntity<CommonResponse<AepsTwoFaStatusDto>> check2FAStatus(
            Aeps2FACheckStatusRequest req) {

        CommonResponse<AepsTwoFaStatusDto> response = new CommonResponse<>();
        AepsTwoFaStatusDto data = new AepsTwoFaStatusDto();

        Optional<AepsTwoFa> optional =
                aAeps2FaRepository.findTopByRetailerIdOrderByIdDesc(req.getRetailerId());

        // 🔹 CASE 3 → 2FA not done
        if (optional.isEmpty()) {
            response.setStatusCode(200);
            response.setMessage("Aadhaar not validated. Please complete 2FA first.");

            data.setTwoFaStatus(3);
            data.setTwoFaData(null);

            response.setData(data);
            return ResponseEntity.ok(response);
        }

        AepsTwoFa logData = optional.get();

        // 🔹 CASE: 2FA failed
        if (!"00".equals(logData.getResponseCode())) {
            response.setStatusCode(200);
            response.setMessage("2FA attempt failed. Please try again.");

            data.setTwoFaStatus(3);
            data.setTwoFaData(logData);

            response.setData(data);
            return ResponseEntity.ok(response);
        }

//        // CASE 4 → 2FA expired
//        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
//        LocalDateTime expiryTime = logData.getCreatedAt().plusHours(24);
//
//        if (now.isAfter(expiryTime)) {
//            response.setStatusCode(200);
//            response.setMessage("2FA expired. Please do 2FA again.");
//
//            data.setTwoFaStatus(4);
//            data.setTwoFaData(logData);
//
//            response.setData(data);
//            return ResponseEntity.ok(response);
//        }

        // CASE 4 → 2FA expired (DATE BASED)
            // today's date (India timezone)
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

        ZonedDateTime txnZdt =
                ZonedDateTime.parse(logData.getTransactionTimestamp(), formatter);

        LocalDate txnDate = txnZdt
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDate();


// if date does NOT match → expired
        if (!txnDate.isEqual(today)) {

            response.setStatusCode(200);
            response.setMessage("2FA expired. Please do 2FA again.");

            data.setTwoFaStatus(4); // expired
            data.setTwoFaData(logData);

            response.setData(data);
            return ResponseEntity.ok(response);
        }


        // 🔹 CASE 5 → 2FA valid
        response.setStatusCode(200);
        response.setMessage("2FA is valid.");

        data.setTwoFaStatus(5);
        data.setTwoFaData(logData);

        response.setData(data);
        return ResponseEntity.ok(response);
    }

}


