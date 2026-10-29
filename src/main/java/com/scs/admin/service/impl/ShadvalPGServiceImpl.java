package com.scs.admin.service.impl;

import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.commons.LoggableVendor;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.*;
import com.scs.admin.request.ShadvalPayCreateOrderRequest;
import com.scs.admin.response.ShadvalPayCreateOrderResponse;
import com.scs.admin.service.ShadvalPGService;
import com.scs.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
@LoggableVendor
public class ShadvalPGServiceImpl implements ShadvalPGService {

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionServiceImpl;

    @Autowired
    private TransactionSchemeRequestEntityRepository transactionSchemeRequestEntityRepository;

    @Autowired
    private ProductTransactionLimitRepository productTransactionLimitRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;


    @Override
    public String createOrder(ShadvalPayCreateOrderRequest req,
                              RequestHeaderMetaDto requestHeaderMetaDto) {

        try {

            // ===== 1. Get Credentials =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            String apiKey = credentials.getToken();
            String merchantId = credentials.getClientId();

            // ===== 2. Create Order ID =====
            String orderId = "ORD" + commonMethods.generateBaseTransactionId();

            // ===== 3. Generate Signature =====
            String plainText = apiKey
                    + merchantId
                    + req.getAmount()
                    + orderId
                    + req.getCustomerName()
                    + req.getEmailId()
                    + req.getMobileNo();

            String signature = generateHmacSHA256Signature(plainText, apiKey);

            // ===== 4. Prepare Request =====
            Map<String, Object> body = new HashMap<>();
            body.put("merchant_id", merchantId);
            body.put("txn_amount", req.getAmount().toString());
            body.put("currency", "INR");
            body.put("txn_unique_id", orderId);
            body.put("customer_name", req.getCustomerName());
            body.put("email_address", req.getEmailId());
            body.put("mobile_number", req.getMobileNo());
            body.put("remarks", req.getRemark());
            body.put("payment_mode", "ALL");
            body.put("callback_url", credentials.getCallbackUrl());

            // IMPORTANT
            body.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", apiKey);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<ShadvalPayCreateOrderResponse> response =
                    restTemplate.exchange(
                            credentials.getBaseUrlUat() + "/generate_invoice",
                            HttpMethod.POST,
                            entity,
                            ShadvalPayCreateOrderResponse.class
                    );

            ShadvalPayCreateOrderResponse res = response.getBody();

            if (res != null && "SUCCESS".equals(res.getStatus())) {

                return res.getData().getPayment_url();

            } else {
                assert res != null;
                throw new RuntimeException("Shadval Order Failed: " + res.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException("Shadval Create Order Failed", e);
        }
    }

    private String generateHmacSHA256Signature(String plainText, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Convert to Base64 (most fintech APIs use Base64)
        return Base64.getEncoder().encodeToString(hash);
    }
}
