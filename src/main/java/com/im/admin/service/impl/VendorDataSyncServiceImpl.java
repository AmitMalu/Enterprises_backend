package com.im.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.VendorApiResponse;
import com.im.admin.dto.VendorBankDTO;
import com.im.admin.dto.VendorPurposeDTO;
import com.im.admin.dto.VendorStateDTO;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VimoPayBankList;
import com.im.admin.entities.VimoPayPurposeList;
import com.im.admin.entities.VimoPayStateList;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.VimoEncryptedRequest;
import com.im.admin.service.VendorDataSyncService;
import com.im.admin.util.VidualPayCryptoUtil;
import com.im.admin.util.VimoPayCryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class VendorDataSyncServiceImpl implements VendorDataSyncService {

    // Token cache (in-memory)
    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);
    private final AtomicReference<Instant> tokenFetchedAt = new AtomicReference<>(null);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private VimoPayCryptoUtil vimoPayCryptoUtil;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private VimoPayBankListRepository vimoPayBankListRepository;

    @Autowired
    private VimoPayStateListRepository vimoPayStateListRepository;

    @Autowired
    private VimoPayPurposeListRepository vimoPayPurposeListRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Public API to trigger one-time sync: token -> banks
     *                                            -> states
     *                                            -> purposes
     */
    public void fetchAndSaveAll(String vendorId) {
        log.info("===== Starting Vendor Data Sync Process =====");
        log.info("Vendor ID: {}", vendorId);
        long startTime = System.currentTimeMillis();

        try {
            // Get base URL
            log.debug("Fetching vendor credentials for vendor ID: {}", vendorId);
            Optional<VendorCredentials> optVendorCredentials = vendorCredentialsRepository.findByVendor_Id(vendorId);

            if (!optVendorCredentials.isPresent()) {
                log.error("Vendor credentials not found for vendor ID: {}", vendorId);
                throw new RuntimeException("Vendor credentials not found for vendor ID: " + vendorId);
            }

            VendorCredentials vendorCredentials = optVendorCredentials.get();
            log.info("Vendor credentials found - Vendor ID: {}, Username: {}",
                    vendorCredentials.getVendor().getId(),
                    vendorCredentials.getUsername());
            log.debug("Base URL UAT: {}", vendorCredentials.getBaseUrlUat());
            log.debug("Base URL Prod: {}", vendorCredentials.getBaseUrlProd());

            String uatBaseUrl = vendorCredentials.getBaseUrlUat();
            log.info("Using UAT Base URL: {}", uatBaseUrl);

            // 1) Get token
            log.info("Step 1: Obtaining authentication token...");
            long tokenStartTime = System.currentTimeMillis();
            String token = obtainToken(vendorCredentials, uatBaseUrl, true);
            long tokenDuration = System.currentTimeMillis() - tokenStartTime;
            log.info("Token obtained successfully in {} ms. Token length: {}",
                    tokenDuration,
                    token != null ? token.length() : 0);
            log.debug("Token (first 20 chars): {}",
                    token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");

            // 2) Fetch each list and save to DB (decrypting responses)
            log.info("Step 2: Fetching and saving data lists...");

            // Fetch Banks
            log.info("--- Starting Bank List Sync ---");
            long bankStartTime = System.currentTimeMillis();
            try {
                fetchAndSaveBanks(vendorCredentials, uatBaseUrl, token);
                long bankDuration = System.currentTimeMillis() - bankStartTime;
                log.info("Bank List Sync completed successfully in {} ms", bankDuration);
            } catch (Exception e) {
                log.error("Bank List Sync failed", e);
                // Continue with other syncs? Or rethrow? Based on your requirement
                // For now, we'll log and continue, but you might want to rethrow
            }

            // Fetch States
            log.info("--- Starting State List Sync ---");
            long stateStartTime = System.currentTimeMillis();
            try {
                fetchAndSaveStates(vendorCredentials, uatBaseUrl, token);
                long stateDuration = System.currentTimeMillis() - stateStartTime;
                log.info("State List Sync completed successfully in {} ms", stateDuration);
            } catch (Exception e) {
                log.error("State List Sync failed", e);
                // Continue with other syncs? Or rethrow? Based on your requirement
            }

            // Fetch Purposes
            log.info("--- Starting Purpose List Sync ---");
            long purposeStartTime = System.currentTimeMillis();
            try {
                fetchAndSavePurposes(vendorCredentials, uatBaseUrl, token);
                long purposeDuration = System.currentTimeMillis() - purposeStartTime;
                log.info("Purpose List Sync completed successfully in {} ms", purposeDuration);
            } catch (Exception e) {
                log.error("Purpose List Sync failed", e);
                // Continue with other syncs? Or rethrow? Based on your requirement
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("===== Vendor Data Sync Process Completed Successfully =====");
            log.info("Total sync duration: {} ms", totalDuration);
            log.info("Sync Summary:");
            log.info("  - Vendor ID: {}", vendorId);
            log.info("  - Token acquired: Yes");
            log.info("  - Banks synced: Check logs for success/failure");
            log.info("  - States synced: Check logs for success/failure");
            log.info("  - Purposes synced: Check logs for success/failure");

        } catch (Exception e) {
            log.error("===== Vendor Data Sync Process Failed =====");
            log.error("Vendor ID: {}", vendorId);
            log.error("Error: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new RuntimeException("Vendor data sync failed for vendor ID: " + vendorId, e);
        }
    }

    /**
     * Obtain token; if forceRefresh==false, try cached token first.
     * If HTTP 401 encountered, and retryOnAuthFail==true, refresh token one time.
     */
    private String obtainToken(VendorCredentials vendorCredentials, String baseUrl, boolean forceRefresh) {
        log.debug("Obtaining token - forceRefresh: {}, baseUrl: {}", forceRefresh, baseUrl);

        if (!forceRefresh) {
            String t = cachedToken.get();
            if (t != null) {
                Instant tokenTime = tokenFetchedAt.get();
                log.debug("Using cached token - Token fetched at: {}, Token length: {}",
                        tokenTime,
                        t.length());
                log.trace("Cached token (first 10 chars): {}",
                        t.substring(0, Math.min(10, t.length())) + "...");
                return t;
            }
            log.debug("No cached token available, proceeding to fetch new token");
        } else {
            log.debug("Force refresh requested, fetching new token");
        }

        synchronized (this) {
            log.debug("Entered synchronized block for token acquisition");

            // double-check
            if (!forceRefresh) {
                String t = cachedToken.get();
                if (t != null) {
                    log.debug("Double-check: Cached token found, returning it");
                    return t;
                }
                log.debug("Double-check: No cached token found");
            }

            log.info("Fetching new token from API...");
            long tokenApiStartTime = System.currentTimeMillis();

            try {
                String token = callTokenApi(vendorCredentials);

                if (token == null || token.isEmpty()) {
                    log.error("Received null or empty token from API");
                    throw new RuntimeException("Received null or empty token from API");
                }

                cachedToken.set(token);
                tokenFetchedAt.set(Instant.now());

                long tokenApiDuration = System.currentTimeMillis() - tokenApiStartTime;
                log.info("New token fetched successfully in {} ms", tokenApiDuration);
                log.debug("New token length: {}, Token (first 10 chars): {}",
                        token.length(),
                        token.substring(0, Math.min(10, token.length())) + "...");
                log.debug("Token stored in cache at: {}", tokenFetchedAt.get());

                return token;

            } catch (Exception e) {
                log.error("Failed to fetch new token from API", e);
                // Clear cache on failure to prevent using invalid token
                cachedToken.set(null);
                tokenFetchedAt.set(null);
                log.warn("Token cache cleared due to API failure");
                throw new RuntimeException("Failed to obtain token", e);
            }
        }
    }

    /**
     * Helper method to log token status
     */
    private void logTokenStatus() {
        String token = cachedToken.get();
        Instant tokenTime = tokenFetchedAt.get();
        if (token != null) {
            log.debug("Token status - Present: true, Length: {}, Fetched at: {}",
                    token.length(), tokenTime);
        } else {
            log.debug("Token status - Present: false");
        }
    }

    /* ------------------------ fetch & save helpers ------------------------ */

    private void fetchAndSaveBanks(VendorCredentials vendorCredentials, String baseUrl, String token) {
        // Attempt with token; on 401 refresh token once and retry
        try {
            VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/banklistuat", token);
            handleEncryptedListResponseForBanks(vendorCredentials, wrapper);
        } catch (WebClientResponseException e) {
            if (e.getRawStatusCode() == 401) {
                log.info("Banks API 401 -> refreshing token and retrying");
                String newToken = obtainToken(vendorCredentials, baseUrl, true);
                VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/banklistuat", newToken);
                handleEncryptedListResponseForBanks(vendorCredentials, wrapper);
            } else {
                throw e;
            }
        }
    }

    private void fetchAndSaveStates(VendorCredentials vendorCredentials, String baseUrl, String token) {
        try {
            VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/statelistuat", token);
            handleEncryptedListResponseForStates(vendorCredentials, wrapper);
        } catch (WebClientResponseException e) {
            if (e.getRawStatusCode() == 401) {
                log.info("States API 401 -> refreshing token and retrying");
                String newToken = obtainToken(vendorCredentials, baseUrl, true);
                VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/statelistuat", newToken);
                handleEncryptedListResponseForStates(vendorCredentials, wrapper);
            } else {
                throw e;
            }
        }
    }

    private void fetchAndSavePurposes(VendorCredentials vendorCredentials, String baseUrl, String token) {
        try {
            VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/purposelistuat", token);
            handleEncryptedListResponseForPurposes(vendorCredentials, wrapper);
        } catch (WebClientResponseException e) {
            if (e.getRawStatusCode() == 401) {
                log.info("Purposes API 401 -> refreshing token and retrying");
                String newToken = obtainToken(vendorCredentials, baseUrl, true);
                VendorApiResponse wrapper = callGetEncrypted(vendorCredentials, baseUrl, "/masterapi/api/master/purposelistuat", newToken);
                handleEncryptedListResponseForPurposes(vendorCredentials, wrapper);
            } else {
                throw e;
            }
        }
    }

    private void handleEncryptedListResponseForBanks(VendorCredentials vendorCredentials, VendorApiResponse wrapper) {
        if (wrapper == null) throw new RuntimeException("Empty wrapper for banks");
        if (!"000".equals(wrapper.getResponseCode())) {
            throw new RuntimeException("Banks API returned: " + wrapper.getMessage());
        }
        String enc = wrapper.getData();
        String decrypted = vimoPayCryptoUtil.decryptFromVendor(vendorCredentials, enc);
        try {
            List<VendorBankDTO> list = objectMapper.readValue(decrypted, new TypeReference<List<VendorBankDTO>>() {});
            // delegate save to your existing service which up serts
            saveVendorBankList(vendorCredentials.getVendor().getId(), list);
        } catch (Exception e) {
            log.error("Failed to parse bank list", e);
            throw new RuntimeException("Failed to parse bank list", e);
        }
    }

    private void handleEncryptedListResponseForStates(VendorCredentials vendorCredentials, VendorApiResponse wrapper) {
        if (wrapper == null) throw new RuntimeException("Empty wrapper for states");
        if (!"000".equals(wrapper.getResponseCode())) {
            throw new RuntimeException("States API returned: " + wrapper.getMessage());
        }
        String enc = wrapper.getData();
        String decrypted = vimoPayCryptoUtil.decryptFromVendor(vendorCredentials, enc);
        try {
            List<VendorStateDTO> list = objectMapper.readValue(decrypted, new TypeReference<List<VendorStateDTO>>() {});
            saveVendorStateList(vendorCredentials.getVendor().getId(), list);
        } catch (Exception e) {
            log.error("Failed to parse state list", e);
            throw new RuntimeException("Failed to parse state list", e);
        }
    }

    private void handleEncryptedListResponseForPurposes(VendorCredentials vendorCredentials, VendorApiResponse wrapper) {
        if (wrapper == null) throw new RuntimeException("Empty wrapper for purposes");
        if (!"000".equals(wrapper.getResponseCode())) {
            throw new RuntimeException("Purposes API returned: " + wrapper.getMessage());
        }
        String enc = wrapper.getData();
        String decrypted = vimoPayCryptoUtil.decryptFromVendor(vendorCredentials, enc);
        try {
            List<VendorPurposeDTO> list = objectMapper.readValue(decrypted, new TypeReference<List<VendorPurposeDTO>>() {});
            saveVendorPurposes(vendorCredentials.getVendor().getId(), list);
        } catch (Exception e) {
            log.error("Failed to parse purpose list", e);
            throw new RuntimeException("Failed to parse purpose list", e);
        }
    }

    public String callTokenApi(VendorCredentials vendorCredentials) {

        log.info("===== Starting Token API call =====");

        try {

            String url = vendorCredentials.getTokenUrlUat();

            HttpHeaders headers = new HttpHeaders();

            headers.set("secretKey", vendorCredentials.getClientSecret());
            headers.set("saltKey", vendorCredentials.getSaltKey());
            headers.set("encryptdecryptKey", vendorCredentials.getClientId());
            headers.set("userId", vendorCredentials.getUsername());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity =
                    new HttpEntity<>(null, headers);

            log.info("[VIMOPAY] Token Header{} -----> ", headers);

            long start = System.currentTimeMillis();

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            log.info("API completed in {} ms",
                    System.currentTimeMillis() - start);

            String body = response.getBody();

            log.info("[VIMOPAY] Token Raw Response={}", body);

            JsonNode root = objectMapper.readTree(body);

            if (!root.path("successStatus").asBoolean()) {
                throw new RuntimeException(
                        "Token API failed : " +
                                root.path("message").asText()
                );
            }

            String token = root.path("data").asText();

            log.info("Token generated successfully");

            return token;

        } catch (Exception ex) {

            log.error("===== Token API call failed =====", ex);

            throw new RuntimeException(
                    "Token API failed",
                    ex
            );
        }
    }   /* ------------------------ generic GET/POST helpers ------------------------ */

    private VendorApiResponse callGetEncrypted(
            VendorCredentials vendorCredentials,
            String baseUrl,
            String path,
            String token) {

        String url = concat(baseUrl, path);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("userId", vendorCredentials.getUsername());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        LocalDateTime requestTime = null;
        LocalDateTime responseTime = null;

        try {

            requestTime = LocalDateTime.now();

            ResponseEntity<String> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            responseTime = LocalDateTime.now();

            String respBody = resp.getBody();
            log.info("Resp Body --------------> {}", respBody);
            int status = resp.getStatusCodeValue();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    path,
                    vendorCredentials.getVendor().getId(),
                    responseTime,
                    respBody,
                    url
            );

            if (status == 401) {
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            }

            if (respBody == null) {
                throw new RuntimeException("Empty response from vendor for " + path);
            }

            return objectMapper.readValue(respBody, VendorApiResponse.class);

        } catch (HttpClientErrorException e) {

            responseTime = LocalDateTime.now();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    path,
                    vendorCredentials.getVendor().getId(),
                    responseTime,
                    e.getResponseBodyAsString(),
                    url
            );

            throw e;

        } catch (Exception e) {

            responseTime = LocalDateTime.now();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    path,
                    vendorCredentials.getVendor().getId(),
                    responseTime,
                    e.getMessage(),
                    url
            );

            throw new RuntimeException(e);
        }
    }

    private VendorApiResponse callPostEncrypted(
            VendorCredentials vendorCredentials,
            String baseUrl,
            String path,
            String token,
            String encryptedPayload,
            String plainJson) {

        String url = concat(baseUrl, path);

        String requestBody = null;
        String respBody = null; // MOVE HERE

        log.info(
                "Calling vendor POST API | vendorId={} | url={}",
                vendorCredentials.getVendor().getId(),
                url
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.set("userId", vendorCredentials.getVendor().getId());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {

            requestBody =
                    objectMapper.writeValueAsString(
                            new VimoEncryptedRequest(encryptedPayload)
                    );

            HttpEntity<String> entity =
                    new HttpEntity<>(
                            requestBody,
                            headers
                    );

            long start =
                    System.currentTimeMillis();

            ResponseEntity<String> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            long duration =
                    System.currentTimeMillis() - start;

            respBody =
                    resp.getBody();

            int status =
                    resp.getStatusCodeValue();

            log.info(
                    "Vendor API response | path={} | status={} | time={}ms",
                    path,
                    status,
                    duration
            );

            commonFunctions.saveVendorLogs(
                    LocalDateTime.now(),
                    requestBody,
                    vendorCredentials.getVendor().getId(),
                    LocalDateTime.now(),
                    respBody,
                    null
            );

            if (status == 401) {
                throw new HttpClientErrorException(
                        HttpStatus.UNAUTHORIZED
                );
            }

            if (respBody == null || respBody.isBlank()) {
                throw new RuntimeException(
                        "Empty response from vendor for " + path
                );
            }

            return objectMapper.readValue(
                    respBody,
                    VendorApiResponse.class
            );

        } catch (HttpStatusCodeException e) {

            respBody =
                    e.getResponseBodyAsString();

            commonFunctions.saveVendorLogs(
                    LocalDateTime.now(),
                    requestBody,
                    vendorCredentials.getVendor().getId(),
                    LocalDateTime.now(),
                    respBody,
                    null
            );

            throw e;

        } catch (Exception e) {

            commonFunctions.saveVendorLogs(
                    LocalDateTime.now(),
                    requestBody,
                    vendorCredentials.getVendor().getId(),
                    LocalDateTime.now(),
                    respBody,
                    null
            );

            throw new RuntimeException(e);
        }

    }

    private String buildTokenRequestPayload(String vendorId) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("requestTime", String.valueOf(System.currentTimeMillis()));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build token request payload", e);
        }
    }

    private void saveVendorBankList(String vendorId, List<VendorBankDTO> banks) {

        Optional<Vendors> vendors = vendorRepository.findById(vendorId);

        for (VendorBankDTO dto : banks) {

            Optional<VimoPayBankList> existing = vimoPayBankListRepository
                    .findByVendor_IdAndBankName(vendorId, dto.getBankName());

            VimoPayBankList bank = existing.orElse(new VimoPayBankList());

            bank.setVendor(vendors.get());
            bank.setBankName(dto.getBankName());
            bank.setBankCode(dto.getBankCode());
            bank.setUpdatedAt(LocalDateTime.now());

            if (bank.getId() == null) {
                bank.setCreatedAt(LocalDateTime.now());
            }

            vimoPayBankListRepository.save(bank);
        }
    }

    private void saveVendorStateList(String vendorId, List<VendorStateDTO> states) {

        Optional<Vendors> vendors = vendorRepository.findById(String.valueOf(vendorId));

        for (VendorStateDTO dto : states) {

            Optional<VimoPayStateList> existing =
                    vimoPayStateListRepository.findByVendor_IdAndStateName(String.valueOf(vendorId), dto.getStateName());

            VimoPayStateList state = existing.orElse(new VimoPayStateList());

            state.setVendor(vendors.get());
            state.setStateName(dto.getStateName());
            state.setStateCode(dto.getStateCode());
            state.setUpdatedAt(LocalDateTime.now());

            if (state.getId() == null) {
                state.setCreatedAt(LocalDateTime.now());
            }

            vimoPayStateListRepository.save(state);
        }
    }

    private void saveVendorPurposes(String vendorId, List<VendorPurposeDTO> purposes) {

        Optional<Vendors> vendors = vendorRepository.findById(String.valueOf(vendorId));

        for (VendorPurposeDTO dto : purposes) {

            Optional<VimoPayPurposeList> existing =
                    vimoPayPurposeListRepository.findByVendor_IdAndPurposeName(String.valueOf(vendorId), dto.getPurposeName());

            VimoPayPurposeList purpose = existing.orElse(new VimoPayPurposeList());

            purpose.setVendor(vendors.get());
            purpose.setPurposeName(dto.getPurposeName());
            purpose.setPurposeCode(dto.getPurposeCode());
            purpose.setUpdatedAt(LocalDateTime.now());

            if (purpose.getId() == null) {
                purpose.setCreatedAt(LocalDateTime.now());
            }

            vimoPayPurposeListRepository.save(purpose);
        }
    }

    private String concat(String base, String path) {
        if (path == null || path.isBlank()) return base;
        if (base.endsWith("/") && path.startsWith("/")) return base + path.substring(1);
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        return base + path;
    }

}
