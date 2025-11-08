package com.im.admin.service.impl;


import com.im.admin.dto.RemitterEkycRequest;
import com.im.admin.dto.RemitterQueryResponse;
import com.im.admin.dto.RemitterRegisterRequest;
import com.im.admin.dto.RemitterRegisterResponse;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.repositories.RemitterRepository;
import com.im.admin.service.RemitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RemitterServiceImpl implements RemitterService {

    @Autowired
    private RemitterRepository remitterRepository;

    @Autowired
    RestTemplate restTemplate;

//    RestTemplate restTemplate = new RestTemplate();

//    @Autowired
//    private RestClient restClient;

    // Vendor URLs
    private final String QUERY_URL = "https://xyz.xyz.in/service-api/api/v1/service/dmt/kyc/remitter/queryremitter";
    private final String KYC_URL = "https://xyz.xyz.in/service-api/api/v1/service/dmt/kyc/remitter/queryremitter/kyc";
    private final String REGISTER_URL = "https://xyz.xyz.in/service-api/api/v1/service/dmt/kyc/remitter/registerremitter";

    private static final String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJQQVlTUFJJTlQiLCJ0aW1lc3RhbXAiOjE2MTAwMjYzMzgsInBhcnRuZXJJZCI6IlBTMDAxIiwicHJvZHVjdCI6IldBTExFVCIsInJlcWlkIjoxNjEwMDI2MzM4fQ.buzD40O8X_41RmJ0PCYbBYx3IBlsmNb9iVmrVH9Ix64";
    private static final String AUTH_KEY = "MzNkYzllOGJmZGVhNWRkZTc1YTgzM2Y5ZDFlY2EyZTQ=";

    @Override
    public Object checkRemitter(String mobile) {

        //  STEP 1: Check if remitter already exists in LOCAL DB
        Optional<DmtRemitter> exist = remitterRepository.findByMobile(mobile);

        if (exist.isPresent() && "REGISTERED".equals(exist.get().getStatus())) {
            return "Remitter already registered and active";
        }

        //  STEP 2: Prepare request body for vendor query API
        Map<String, Object> reqBody = Map.of("mobile", mobile);

        HttpHeaders headers = getHeaders();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(reqBody, headers);

        //  STEP 3: Call Vendor Remitter Query API
        ResponseEntity<RemitterQueryResponse> responseEntity =
                restTemplate.exchange(QUERY_URL, HttpMethod.POST, httpEntity, RemitterQueryResponse.class);

        RemitterQueryResponse res = responseEntity.getBody();
        int code = res.getResponse_code();

        //  STEP 4: Handle Vendor Response Cases
        switch (code) {

            case 1: //  Remitter already exists on vendor side
                saveRemitter(res); // Save in our DB as REGISTERED
                return "Remitter exists. You can proceed";

            case 2: //  KYC required
                return "EKYC_REQUIRED";

            case 3: //  OTP sent → Start registration -> INSERT temp record in DB
                saveTempRecord(res);
                return res;

            default: //  Error cases from vendor
                return res.getMessage();
        }
    }

    @Override
    public Object doEkyc(RemitterEkycRequest request) {

        //  STEP 5: Do E-KYC API call
        HttpHeaders headers = getHeaders();
        HttpEntity<RemitterEkycRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> resp =
                restTemplate.exchange(KYC_URL, HttpMethod.POST, entity, Map.class);

        return resp.getBody();
    }

    @Override
    public Object registerRemitter(RemitterRegisterRequest req) {

        //  STEP 6: Call Register Remitter API (after OTP verification)
        HttpHeaders headers = getHeaders();
        HttpEntity<RemitterRegisterRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<RemitterRegisterResponse> respEntity =
                restTemplate.exchange(REGISTER_URL, HttpMethod.POST, entity, RemitterRegisterResponse.class);

        RemitterRegisterResponse resp = respEntity.getBody();

        //  STEP 7: Update our DB after successful registration
        if (resp.getResponse_code() == 1) {
            DmtRemitter r = remitterRepository.findByMobile(req.getMobile()).get();
            r.setStatus("REGISTERED");
            r.setEkycId(null);
            r.setStateResp(null);
            remitterRepository.save(r);
        }

        return resp;
    }

    // Common method to set headers
//    private HttpHeaders getHeaders(){
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Token", TOKEN);
//        headers.set("Authorisedkey", AUTH_KEY);
//        return headers;
//    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", TOKEN);
        headers.set("Authorisedkey", AUTH_KEY);
        return headers;
    }


    //  Save if remitter already exists
    private void saveRemitter(RemitterQueryResponse res){
        DmtRemitter en = new DmtRemitter();
        en.setMobile(res.getData().getMobile());
        en.setStatus("REGISTERED");
        remitterRepository.save(en);
    }

    //  Save temp entry if OTP registration required
    private void saveTempRecord(RemitterQueryResponse res){
        DmtRemitter en = new DmtRemitter();
        en.setMobile(res.getData().getMobile());
        en.setEkycId(res.getData().getEkyc_id());
        en.setStateResp(res.getData().getStateresp());
        en.setStatus("PENDING"); // Not registered yet
        remitterRepository.save(en);
    }

}

