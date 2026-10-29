package com.scs.admin.service.impl;

/**
 * @author SHUBHAM KHOPADE
 */
import com.scs.admin.request.DigilockerInitRequest;
import com.scs.admin.response.DigilockerAadhaarResponse;
import com.scs.admin.response.DigilockerInitResponse;
import com.scs.admin.service.DigilockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DigilockerServiceImpl implements DigilockerService {

    private final RestTemplate restTemplate;

    @Value("${surepass.digilocker.token}")
    private String digilockerToken;

    private static final String INIT_URL =
            "https://kyc-api.surepass.app/api/v1/digilocker/initialize";

    @Override
    public DigilockerInitResponse initializeDigilocker() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(digilockerToken);

        Map<String, Object> data = new HashMap<>();

        // mandatory field
        data.put("signup_flow", true);
        data.put("skip_main_screen", false);

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<DigilockerInitResponse> response =
                restTemplate.exchange(
                        INIT_URL,
                        HttpMethod.POST,
                        entity,
                        DigilockerInitResponse.class
                );

        return response.getBody();
    }

    @Override
    public DigilockerAadhaarResponse downloadAadhaar(String clientId) {

        String url = "https://kyc-api.surepass.app/api/v1/digilocker/download-aadhaar/" + clientId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(digilockerToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<DigilockerAadhaarResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        DigilockerAadhaarResponse.class
                );

        return response.getBody();
    }
}
