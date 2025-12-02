package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FingPayAepsServiceImpl implements AepsVendorService {

    private static final String FINGPAY_CW_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/cashWithdrawal/merchant/withdrawal";

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ResourceLoader resourceLoader;
    @Autowired private RestTemplate restTemplate;

    @Override
    public String getVendorCode() {
        return "fingpay aeps";
    }

    @Override
    public ResponseEntity<AepsCashWithdrawalResponse> processCashWithdrawal(
            AepsCashWithdrawalRequest req, VendorCredentials credentials) {

        try {
            String json = objectMapper.writeValueAsString(req);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(json, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", req.getDeviceIMEI() != null ? req.getDeviceIMEI() : "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            return restTemplate.exchange(
                    FINGPAY_CW_URL,
                    HttpMethod.POST,
                    entity,
                    AepsCashWithdrawalResponse.class
            );

        } catch (Exception e) {
            e.printStackTrace();
            AepsCashWithdrawalResponse dto = new AepsCashWithdrawalResponse(false, "Exception: " + e.getMessage(), 500L, null);
            return ResponseEntity.ok(dto);
        }
    }

    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {

        try {
            String json = objectMapper.writeValueAsString(req);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(json, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", req.getDeviceIMEI() != null ? req.getDeviceIMEI() : "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            return restTemplate.exchange(
                    "https://fingpayap.tapits.in/fpaepsservice/api/balanceInquiry/merchant/getBalance",
                    HttpMethod.POST,
                    entity,
                    AepsBalanceInquiryResponse.class
            );

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
