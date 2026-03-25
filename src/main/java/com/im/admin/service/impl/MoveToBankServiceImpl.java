package com.im.admin.service.impl;

import com.im.admin.controller.AESController;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.EncryptDecryptCredentialsDto;
import com.im.admin.dto.PayoutTokenResponseDTO;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.MoveToBankService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MoveToBankServiceImpl implements MoveToBankService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private AESController aesController;

    RestTemplate restTemplate = new RestTemplate();


    @Override
    @Transactional
    public CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest, Double latitude, Double longitude) {
        try {

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            Optional<SubProducts> subProducts = subProductsRepository.findById(sendPayoutRequest.getSubProductId());
            WalletType walletType = WalletType.valueOf(subProducts.get().getWalletType());

            BigDecimal availableBalance = commonMethods.getCurrentBalance(sendPayoutRequest.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(sendPayoutRequest.getAmount()) < 0) {
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(sendPayoutRequest.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            log.debug("Vendor credentials retrieved: {}", credentials.getVendor().getVendorName());

            // ====== Check Vendor Type ======
            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {
                // =========================
                // AES Flow (Admin Panel)
                // =========================
                String token = generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS

                SendPayoutRequest payoutJsonRequestBody = new SendPayoutRequest();

                payoutJsonRequestBody.setAccountNumber(sendPayoutRequest.getAccountNumber());
                payoutJsonRequestBody.setIfscCode(sendPayoutRequest.getIfscCode());
                payoutJsonRequestBody.setAmount(sendPayoutRequest.getAmount());
                payoutJsonRequestBody.setOrderId("TXN" + baseTransactionId);
                payoutJsonRequestBody.setBeneficiaryName(sendPayoutRequest.getBeneficiaryName());
                payoutJsonRequestBody.setPaymentMode(sendPayoutRequest.getPaymentMode());
                payoutJsonRequestBody.setCallbackurl(sendPayoutRequest.getCallbackurl());

                HttpEntity<SendPayoutRequest> requestEntity = new HttpEntity<>(payoutJsonRequestBody, headers);

                String url = credentials.getBaseUrlUat() + "/payment";
                log.info("Sending payout request to RunPaisa. URL: {}, Headers: {}, Body: {}", url, requestEntity, String.class);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

            } else {

                AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), sendPayoutRequest.getRetailerId());
                commonMethods.updateWallet(walletAmountTable, sendPayoutRequest.getRetailerId(), walletType, sendPayoutRequest.getAmount(), "Debit", "TXN" + baseTransactionId, "VENDOR_PENDING", "DEBIT", "Move To Bank", sendPayoutRequest.getBeneficiaryName() + "#" + sendPayoutRequest.getAccountNumber(), "Fund Transferred " + "#" + sendPayoutRequest.getAccountNumber(),"", latitude, longitude);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", generateTokenForPayout(credentials));

                Map<String, Object> payout = new LinkedHashMap<>();
                payout.put("account_number", String.valueOf(sendPayoutRequest.getAccountNumber()));  // "026291800001191"
                payout.put("ifsc_code", String.valueOf(sendPayoutRequest.getIfscCode()));            // "YESB0000001"
                payout.put("amount", String.format("%.2f", sendPayoutRequest.getAmount()));          // "11.00"
                payout.put("order_id", "TXN" + baseTransactionId);                                   // "TXN25082012270100272"
                payout.put("beneficiary_name", String.valueOf(sendPayoutRequest.getBeneficiaryName()));
                payout.put("payment_mode", String.valueOf(sendPayoutRequest.getPaymentMode()));      // "IMPS"

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payout, headers);

                String url = credentials.getBaseUrlUat() + "/payment";
                log.info("Sending payout request to RunPaisa. URL: {}, Headers: {}, Body: {}", url, headers, payout);

                ResponseEntity<PayoutResponseDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        requestEntity,
                        PayoutResponseDto.class
                );

                PayoutResponseDto payoutResponse = response.getBody();
                assert payoutResponse != null;
                log.info("RunPaisa payout response: {}", payoutResponse.getMessage());


                if (payoutResponse != null && "ACCEPTED".equalsIgnoreCase(payoutResponse.getStatus())) {
                    return new CommonResponse<>(200, payoutResponse.getMessage(), payoutResponse);
                } else {
                    String errorMsg = payoutResponse != null ? payoutResponse.getMessage() : "Null response from vendor";
                    return new CommonResponse<>(400, errorMsg, payoutResponse);
                }
            }

        } catch (HttpStatusCodeException e) {
            log.error("Vendor returned HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new CommonResponse<>(e.getStatusCode().value(), "Vendor Error", null);
        } catch (RestClientException e) {
            log.error("Vendor communication error: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Vendor communication failed", null);
        } catch (Exception e) {
            log.error("General exception: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Exception occurred during payout", null);
        }
        return new CommonResponse<>(500, "Something went wrong", null);

    }

    public String generateTokenForPayout(VendorCredentials vendorCredentials) {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", vendorCredentials.getClientId());
        headers.set("username", vendorCredentials.getUsername());
        headers.set("password", vendorCredentials.getPassword());

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = vendorCredentials.getTokenUrlUat();
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PayoutTokenResponseDTO> response = restTemplate.postForEntity(
                url + "/token",
                requestEntity,
                PayoutTokenResponseDTO.class
        );

        PayoutTokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed To Get Token From Payout Vendor");
        }
    }

    private String generateTokenForAES(VendorCredentials vendorCredentials) {
        // Call AES encrypt service
        ResponseEntity<?> responseEntity = aesController.encryptCredentials(
                new EncryptDecryptCredentialsDto(vendorCredentials.getUsername(), vendorCredentials.getPassword())
        );

        @SuppressWarnings("unchecked")
        Map<String, String> encryptedMap = (Map<String, String>) responseEntity.getBody();
        String encUser = encryptedMap.get("cipherUsername");
        String encPass = encryptedMap.get("cipherPassword");
        String salt = encryptedMap.get("salt");
        String iv = encryptedMap.get("iv");

        // Build JSON for token request
        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("username", encUser);
        jsonBody.put("password", encPass);
        jsonBody.put("client_id", vendorCredentials.getClientId());
        jsonBody.put("salt", salt);
        jsonBody.put("ivBase64", iv);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(vendorCredentials.getTokenUrlUat(), requestEntity, Map.class);
        Map<String, Object> responseBodyMap = response.getBody();
        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
        }

        return (String) responseBodyMap.get("token");
    }











}
