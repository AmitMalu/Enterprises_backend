package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.Retailer;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.response.Aeps2FaResponseDto;
import com.im.admin.service.AepsTwoFactorService;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Override
    public ResponseEntity<Aeps2FaResponseDto> validateAadhar(Aeps2FaRequest req) {
        try {

            // Load retailer from DB
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // Backend auto-fill mandatory fields
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

            // Build cardnumberORUID from backend
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

            // Encrypt request
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            // API Request
            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            ResponseEntity<Aeps2FaResponseDto> response = restTemplate.exchange(
                    FINGPAY_2FA_URL,
                    HttpMethod.POST,
                    entity,
                    Aeps2FaResponseDto.class
            );

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            Aeps2FaResponseDto dto = new Aeps2FaResponseDto();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500);
            return ResponseEntity.ok(dto);
        }
    }

}
