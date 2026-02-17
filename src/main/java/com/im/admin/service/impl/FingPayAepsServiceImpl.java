package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.request.AepsMiniStatementRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.AepsMiniStatementResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;

@Service
public class FingPayAepsServiceImpl implements AepsVendorService {

    @Autowired
    private RetailerRepository retailerRepository;

    private static final String SUPER_USERNAME = "nftpayd";
    private static final String MERCHANT_MD5_PIN = "796c3ee556ac31f3754a38cfd15b8044";  // 81dc9bdb52d04dc20036dbd8313ed055  //1234d
    private static final int SUPER_MERCHANT_ID = 1396;


    private static final String FINGPAY_CW_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/cashWithdrawal/merchant/withdrawal";

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public String getVendorCode() {
        return "fingpay aeps";
    }

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(
            AepsCashWithdrawalRequest req, VendorCredentials credentials) {

        VendorRouting vendorRouting =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);


        // ===== Vendor Log Variables =====
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String errorMsg = null;
        String vendorId = vendor.getId();

        try {
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            Map<String, Object> body = new HashMap<>();

            body.put("merchantTranId", "CW-" + System.currentTimeMillis());
            body.put("captureResponse", req.getCaptureResponse());

            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("adhaarNumber", req.getCardnumberORUID().getAdhaarNumber());
            cardMap.put("indicatorforUID", 0);
            cardMap.put("nationalBankIdentificationNumber", 607153);
//            cardMap.put(
//                    "nationalBankIdentificationNumber",
//                    req.getCardnumberORUID().getNationalBankIdentificationNumber()
//            );
            body.put("cardnumberORUID", cardMap);

            body.put("languageCode", "en");
            body.put("serviceType", "AEPS");
            body.put("latitude", req.getLatitude());
            body.put("longitude", req.getLongitude());
            body.put("mobileNumber", req.getMobileNumber());
            body.put("paymentType", "B");
            body.put("requestRemarks", "Test");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String timestamp = sdf.format(new Date());

            body.put("timestamp", timestamp);
            body.put("transactionAmount", req.getTransactionAmount());
            body.put("transactionType", "CW");

            body.put("merchantUserName", retailer.getRetailerId());
//            body.put("subMerchantId", SUPER_USERNAME);
            body.put("merchantPin", MERCHANT_MD5_PIN);
//            body.put("merchantLoginId", req.getMerchantLoginId());
            body.put("superMerchantId", SUPER_MERCHANT_ID);
            body.put("isFacialTan",req.getIsFacialTan());


            requestJson = objectMapper.writeValueAsString(body);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(requestJson, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "860746040494350");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            ResponseEntity<AepsCashWithdrawalResponse> vendorResp =
                    restTemplate.exchange(
                            FINGPAY_CW_URL,
                            HttpMethod.POST,
                            entity,
                            AepsCashWithdrawalResponse.class
                    );

            responseJson = objectMapper.writeValueAsString(vendorResp.getBody());

            //  SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return new CommonResponse<>(200, "Vendor Response Received", vendorResp.getBody());

        } catch (Exception e) {

            errorMsg = e.getMessage();

            try {
                responseJson = objectMapper.writeValueAsString(
                        new AepsCashWithdrawalResponse(false, errorMsg, 500L, null)
                );
            } catch (Exception ignored) {
            }

            //  FAILURE LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    errorMsg
            );

            return new CommonResponse<>(
                    500,
                    "Exception occurred",
                    new AepsCashWithdrawalResponse(false, errorMsg, 500L, null)
            );
        }
    }


    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


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

    @Override
    public ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


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
                    "https://fingpayap.tapits.in/fpaepsservice/api/miniStatement/merchant/statement",
                    HttpMethod.POST,
                    entity,
                    AepsMiniStatementResponse.class
            );

        } catch (Exception e) {
            e.printStackTrace();
            AepsMiniStatementResponse dto = new AepsMiniStatementResponse();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500L);
            return ResponseEntity.ok(dto);
        }
    }
}
