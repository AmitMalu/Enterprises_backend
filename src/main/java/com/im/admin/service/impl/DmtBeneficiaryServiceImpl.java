package com.im.admin.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.DmtBeneficiaryDeleteResponse;
import com.im.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.im.admin.dto.DmtBeneficiaryFetchResponse;
import com.im.admin.entities.DmtBeneficiary;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.BeneficiaryRepository;
import com.im.admin.repositories.RemitterRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.*;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DmtBeneficiaryService;
import com.im.admin.util.CommonMethods;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class DmtBeneficiaryServiceImpl implements DmtBeneficiaryService {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RemitterRepository remitterRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired private ObjectMapper objectMapper;

    @Override
    public CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(BeneficiaryRegisterRequest req) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        vendorId = vendor.getId();

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // ===== STEP 2: Prepare request JSON for logs =====
        requestJson = objectMapper.writeValueAsString(req);

            String token = generateJwtToken();

        // ====== STEP 2: Prepare Headers and Body ======
        HttpHeaders headers = getHeaders(credentials,token);
        HttpEntity<BeneficiaryRegisterRequest> entity = new HttpEntity<>(req, headers);

        // ====== STEP 3: Call Vendor API ======
        ResponseEntity<BeneficiaryRegisterResponse> respEntity = restTemplate.exchange(
                //uat
//                credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary",
                //live
                credentials.getBaseUrlProd() + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary",
                HttpMethod.POST,
                entity,
                BeneficiaryRegisterResponse.class
        );

        BeneficiaryRegisterResponse resp = respEntity.getBody();

        responseJson = objectMapper.writeValueAsString(resp);

        //  SUCCESS VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                responseJson,
                null
        );

        // ====== STEP 4: If successful → save in local DB ======
        if (resp != null && resp.getResponse_code() == 1) {

            DmtRemitter dmtRemitter = remitterRepository.findByMobile(req.getMobile())
                    .orElseThrow(() -> new RuntimeException("Remitter not found for mobile: " + req.getMobile()));

            Optional<DmtBeneficiary> existing =
                    beneficiaryRepository.findByRemitterAndBeneId(
                            dmtRemitter,
                            resp.getData().getBene_id()
                    );

            if (existing.isPresent()) {

                CommonResponse<BeneficiaryRegisterResponse> common = new CommonResponse<>();
                common.setStatusCode(200);
                common.setMessage("Beneficiary already registered");
                common.setData(resp);

                return common;
            }



            DmtBeneficiary bene = new DmtBeneficiary();
            bene.setRemitter(dmtRemitter);
            bene.setVendor(vendor);
            bene.setBeneId(resp.getData().getBene_id());
            bene.setName(req.getBenename());
            bene.setBankId(req.getBankid());
//            bene.setBankName(resp.getData().getBankname());
            bene.setBankName(req.getBankName());
            bene.setAccountNo(req.getAccno());
            bene.setIfscCode(req.getIfsccode());
            bene.setVerified(req.getVerified());
            bene.setDob(req.getDob());
            bene.setAddress(req.getAddress());
            bene.setPincode(req.getPincode());
            bene.setGst_state(req.getGst_state());
            bene.setStatus("REGISTERED");
            beneficiaryRepository.save(bene);
        }

        // ====== STEP 5: Wrap in CommonResponse ======
        CommonResponse<BeneficiaryRegisterResponse> common = new CommonResponse<>();
        common.setStatusCode(200);
        common.setMessage(resp != null ? resp.getMessage() : "Registration failed");
        common.setData(resp);

        return common;

    } catch (Exception ex) {

        //  FAILURE VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                null,
                ex.getMessage()
        );

        log.error(
                "[REGISTER BENEFICIARY] Exception | mobile={}",
                req.getMobile(),
                ex
        );

            throw ex; //
    }
}

    // Helper method to build headers
    private HttpHeaders getHeaders(VendorCredentials credentials,String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        //uat
//        headers.set("Token", credentials.getToken());
        //live
        headers.set("Token",token);
        headers.set("Authorisedkey", credentials.getSaltKey());
        return headers;
    }


    @Override
    public CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(DmtBeneficiaryDeleteRequest req) throws Exception {
        {

            LocalDateTime requestTime = LocalDateTime.now();
            String requestJson = null;
            String responseJson = null;
            String vendorId = null;

            try {
                // Step 1: Load vendor credentials dynamically
                VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
                Vendors vendor = CommonMethods.getVendors(vendorRouting);

                vendorId = vendor.getId();

                VendorCredentials credentials = vendorCredentialsRepository
                        .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("No credentials found for vendor"));

                requestJson = objectMapper.writeValueAsString(req);

                String token = generateJwtToken();

                // Step 2: Prepare headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//                headers.set("Token", credentials.getToken());
                headers.set("Token", token);
                headers.set("Authorisedkey", credentials.getSaltKey());

                // Step 3: Prepare request body
                HttpEntity<DmtBeneficiaryDeleteRequest> entity = new HttpEntity<>(req, headers);

                // Step 4: Call Vendor Delete Beneficiary API
                ResponseEntity<DmtBeneficiaryDeleteResponse> responseEntity = restTemplate.exchange(
                        //uat
//                        credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/deletebeneficiary",
                        //live
                        credentials.getBaseUrlProd() + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/deletebeneficiary",
                        HttpMethod.POST,
                        entity,
                        DmtBeneficiaryDeleteResponse.class
                );

                DmtBeneficiaryDeleteResponse vendorResponse = responseEntity.getBody();

                responseJson = objectMapper.writeValueAsString(vendorResponse);

                //  SUCCESS VENDOR LOG
                commonFunctions.saveVendorLogs(
                        requestTime,
                        requestJson,
                        vendorId,
                        LocalDateTime.now(),
                        responseJson,
                        null
                );

                // Step 5: Update Local Database if deleted successfully
                if (vendorResponse != null && vendorResponse.getResponse_code() == 1) {
                    DmtBeneficiary bene = beneficiaryRepository.findByBeneId(req.getBene_id());
                    if (bene != null) {
                        beneficiaryRepository.delete(bene); // <- DELETE RECORD
                    }
                }

                // Step 6: Wrap final response
                CommonResponse<DmtBeneficiaryDeleteResponse> common = new CommonResponse<>();
                common.setStatusCode(responseEntity.getStatusCodeValue());
                common.setMessage(vendorResponse != null ? vendorResponse.getMessage() : "No response from vendor");
                common.setData(vendorResponse);

                return common;

            } catch (Exception ex) {

                //  FAILURE VENDOR LOG
                commonFunctions.saveVendorLogs(
                        requestTime,
                        requestJson,
                        vendorId,
                        LocalDateTime.now(),
                        null,
                        ex.getMessage()
                );

                log.error(
                        "[DELETE BENEFICIARY] Exception | beneId={}",
                        req.getBene_id(),
                        ex
                );

                throw ex;
            }
        }
    }

//    @Override
//    public CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> fetchBeneficiary(DmtBeneficiaryFetchRequest req, int page, int size) {
//
//        LocalDateTime requestTime = LocalDateTime.now();
//        String requestJson = null;
//        String responseJson = null;
//        String vendorId = null;
//
//        try {
//
//            Optional<DmtRemitter> optionalRemitter = remitterRepository.findByMobile(req.getMobile());
//
//            if (optionalRemitter.isEmpty()) {
//                return CommonResponse.handleErrors("Remitter not found");
//            }
//
//            DmtRemitter remitter = optionalRemitter.get();
//            // STEP 2 → Fetch Local Beneficiaries
//            // ==============================
//            List<DmtBeneficiary> localList = beneficiaryRepository.findByRemitter(remitter);
//
//            if (!localList.isEmpty()) {
//
//                // Convert Entity → DTO
//                List<DmtBeneficiaryFetchResponse.BeneficiaryData> dtoList =
//                        localList.stream().map(this::convertToDto).toList();
//
//
//
//                // Apply pagination using PageImpl
//                int start = (int) PageRequest.of(page, size).getOffset();
//                int end = Math.min(start + size, dtoList.size());
//                List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedData = dtoList.subList(start, end);
//
//                Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pageResponse =
//                        new PageImpl<>(pagedData, PageRequest.of(page, size), dtoList.size());
//
//                return CommonResponse.success(pageResponse, "Beneficiaries fetched");
//            }
//            // ====== STEP 1: Load Vendor Credentials Dynamically ======
//            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
//            Vendors vendor = CommonMethods.getVendors(vendorRouting);
//
//
//            vendorId = vendor.getId();
//
//            VendorCredentials credentials = vendorCredentialsRepository
//                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
//                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));
//
//            String token = generateJwtToken();
//            // ====== STEP 2: Prepare Request ======
//            Map<String, Object> reqBody = Map.of("mobile", req.getMobile());
//            HttpHeaders headers = getHeaders(credentials,token);
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
//
//            // ====== STEP 3: Call Vendor API ======
//            ResponseEntity<DmtBeneficiaryFetchResponse> response = restTemplate.exchange(
//                    //uat
////                    credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiary",
//                    //live
//                    credentials.getBaseUrlProd() + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiary",
//                    HttpMethod.POST,
//                    entity,
//                    DmtBeneficiaryFetchResponse.class
//            );
//
//            DmtBeneficiaryFetchResponse resp = response.getBody();
//
//            responseJson = objectMapper.writeValueAsString(resp);
//
//            //  SUCCESS VENDOR LOG
//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    responseJson,
//                    null
//            );
//
//
//            if (resp == null) {
//                return CommonResponse.handleErrors("No response from vendor");
//            }
//
//            // ====== STEP 4: Handle Response & Apply Pagination ======
//            if (resp.getResponse_code() == 1 && resp.getData() != null) {
//
//                List<DmtBeneficiaryFetchResponse.BeneficiaryData> beneficiaries = resp.getData();
//
//                // Apply manual pagination (since data is fetched externally)
//                int start = (int) PageRequest.of(page, size).getOffset();
//                int end = Math.min(start + size, beneficiaries.size());
//                List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedList = beneficiaries.subList(start, end);
//
//                Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedResponse =
//                        new PageImpl<>(pagedList, PageRequest.of(page, size), beneficiaries.size());
//
//                return CommonResponse.success(pagedResponse, "Beneficiaries fetched successfully");
//            } else {
//                return CommonResponse.handleErrors(resp.getMessage());
//            }
//
//
//        } catch (Exception ex) {
//
//            //  FAILURE VENDOR LOG (only if vendor was involved)
//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    null,
//                    ex.getMessage()
//            );
//
//            log.error(
//                    "[FETCH BENEFICIARY] Exception | mobile={}",
//                    req.getMobile(),
//                    ex
//            );
//
//            return CommonResponse.handleErrors(
//                    "Error fetching beneficiary list: " + ex.getMessage()
//            );
//        }
//    }
    @Override
     public CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> fetchBeneficiary(
        DmtBeneficiaryFetchRequest req, int page, int size) {

    LocalDateTime requestTime = LocalDateTime.now();
    String requestJson = null;
    String responseJson = null;
    String vendorId = null;

    try {

        Optional<DmtRemitter> optionalRemitter = remitterRepository.findByMobile(req.getMobile());

        if (optionalRemitter.isEmpty()) {
            return CommonResponse.handleErrors("Remitter not found");
        }

        DmtRemitter remitter = optionalRemitter.get();

        // ==============================
        // STEP 2 → Fetch Local Beneficiaries
        // ==============================
//        List<DmtBeneficiary> localList = beneficiaryRepository.findByRemitter(remitter);

        List<DmtBeneficiary> localList =
                beneficiaryRepository.findByRemitterOrderByIdDesc(remitter);


        if (!localList.isEmpty()) {

            // Convert Entity → DTO
            List<DmtBeneficiaryFetchResponse.BeneficiaryData> dtoList =
                    localList.stream().map(this::convertToDto).toList();

            // 🔹 APPLY FILTERS (NEW)
            List<DmtBeneficiaryFetchResponse.BeneficiaryData> filteredList =
                    applyFilters(dtoList, req);

            // Apply pagination
            int start = (int) PageRequest.of(page, size).getOffset();
            int end = Math.min(start + size, filteredList.size());

            List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedData =
                    filteredList.subList(start, end);

            Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pageResponse =
                    new PageImpl<>(pagedData, PageRequest.of(page, size), filteredList.size());

            return CommonResponse.success(pageResponse, "Beneficiaries fetched");
        }

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        vendorId = vendor.getId();

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        String token = generateJwtToken();

        // ====== STEP 2: Prepare Request ======
        Map<String, Object> reqBody = Map.of("mobile", req.getMobile());
        HttpHeaders headers = getHeaders(credentials, token);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        // ====== STEP 3: Call Vendor API ======
        ResponseEntity<DmtBeneficiaryFetchResponse> response = restTemplate.exchange(
                credentials.getBaseUrlProd()
                        + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiary",
                HttpMethod.POST,
                entity,
                DmtBeneficiaryFetchResponse.class
        );

        DmtBeneficiaryFetchResponse resp = response.getBody();
        responseJson = objectMapper.writeValueAsString(resp);

        // SUCCESS VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                responseJson,
                null
        );

        if (resp == null) {
            return CommonResponse.handleErrors("No response from vendor");
        }

        // ====== STEP 4: Handle Response & Apply Pagination ======
        if (resp.getResponse_code() == 1 && resp.getData() != null) {

            List<DmtBeneficiaryFetchResponse.BeneficiaryData> beneficiaries = resp.getData();

            // 🔹 APPLY FILTERS (NEW)
            List<DmtBeneficiaryFetchResponse.BeneficiaryData> filteredList =
                    applyFilters(beneficiaries, req);

            int start = (int) PageRequest.of(page, size).getOffset();
            int end = Math.min(start + size, filteredList.size());

            List<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedList =
                    filteredList.subList(start, end);

            Page<DmtBeneficiaryFetchResponse.BeneficiaryData> pagedResponse =
                    new PageImpl<>(pagedList, PageRequest.of(page, size), filteredList.size());

            return CommonResponse.success(pagedResponse, "Beneficiaries fetched successfully");
        } else {
            return CommonResponse.handleErrors(resp.getMessage());
        }

    } catch (Exception ex) {

        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                null,
                ex.getMessage()
        );

        log.error("[FETCH BENEFICIARY] Exception | mobile={}", req.getMobile(), ex);

        return CommonResponse.handleErrors(
                "Error fetching beneficiary list: " + ex.getMessage()
        );
    }
}


    private List<DmtBeneficiaryFetchResponse.BeneficiaryData> applyFilters(
            List<DmtBeneficiaryFetchResponse.BeneficiaryData> list,
            DmtBeneficiaryFetchRequest req) {

        return list.stream()
                .filter(b -> req.getBeneId() == null
                        || b.getBene_id().equals(req.getBeneId()))

                .filter(b -> req.getBankid() == null
                        || b.getBankid().equals(req.getBankid()))

                .filter(b -> req.getBankname() == null
                        || b.getBankname().toLowerCase()
                        .contains(req.getBankname().toLowerCase()))

                .filter(b -> req.getName() == null
                        || b.getName().toLowerCase()
                        .contains(req.getName().toLowerCase()))

                .filter(b -> req.getAccno() == null
                        || b.getAccno().equals(req.getAccno()))

                .filter(b -> req.getIfsc() == null
                        || b.getIfsc().equalsIgnoreCase(req.getIfsc()))

                .filter(b -> req.getVerified() == null
                        || b.getVerified().equals(req.getVerified()))

                .filter(b -> req.getPaytm() == null
                        || b.isPaytm() == req.getPaytm())

                .toList();
    }




    @Override
    public CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


        try {
            // ====== STEP 1: Load Vendor Credentials Dynamically ======
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ====== STEP 2: Prepare Request Body ======
            Map<String, Object> reqBody = Map.of(
                    "mobile", req.getMobile(),
                    "beneid", req.getBeneid()
            );

            requestJson = objectMapper.writeValueAsString(reqBody);

            String token = generateJwtToken();

            HttpHeaders headers = getHeaders(credentials,token);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // ====== STEP 3: Call Vendor API ======
            ResponseEntity<DmtBeneficiaryFetchByBeneIdResponse> response = restTemplate.exchange(
                    //uat
//                    credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiarybybeneid",
                    //live
                    credentials.getBaseUrlProd() + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiarybybeneid",
                    HttpMethod.POST,
                    entity,
                    DmtBeneficiaryFetchByBeneIdResponse.class
            );

            DmtBeneficiaryFetchByBeneIdResponse resp = response.getBody();

            responseJson = objectMapper.writeValueAsString(resp);

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (resp == null) {
                return CommonResponse.handleErrors("No response from vendor");
            }

            // ====== STEP 4: Handle Response ======
            if (resp.getResponse_code() == 1) {
                return CommonResponse.success(resp, "Beneficiary fetched successfully");
            } else {
                return CommonResponse.handleErrors(resp.getMessage());
            }

        } catch (Exception ex) {

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error(
                    "[FETCH BENEFICIARY BY BENE ID] Exception | mobile={}, beneId={}",
                    req.getMobile(),
                    req.getBeneid(),
                    ex
            );

            return CommonResponse.handleErrors(
                    "Error fetching beneficiary details: " + ex.getMessage()
            );
        }
    }


    @Override
    public CommonResponse<DmtBeneficiaryResponseDto> updateBeneficiary(BeneficiaryUpdateRequest req) {
        try {

            // ===== Load vendor credentials =====
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ===== Update local DB =====
            DmtBeneficiary beneficiary = beneficiaryRepository.findByBeneId(req.getBeneId());
            if (beneficiary == null) {
                return CommonResponse.handleErrors("Beneficiary not found with ID: " + req.getBeneId());
            }

            beneficiary.setName(req.getName());
            beneficiary.setBankName(req.getBankname());
            beneficiary.setVerified(req.getVerified());

            DmtBeneficiary updated = beneficiaryRepository.save(beneficiary);

            // Convert entity to DTO
            DmtBeneficiaryResponseDto dto = mapToDto(updated);

            return CommonResponse.success(dto, "Beneficiary updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return CommonResponse.handleErrors("Error updating beneficiary: " + e.getMessage());
        }
    }


    public DmtBeneficiaryResponseDto mapToDto(DmtBeneficiary ben) {
        DmtBeneficiaryResponseDto dto = new DmtBeneficiaryResponseDto();
        dto.setId(ben.getId());
//        dto.setBeneId(ben.getBeneId());
        dto.setBeneId(ben.getBeneId());
        dto.setName(ben.getName());
        dto.setBankId(ben.getBankId());
        dto.setBankName(ben.getBankName());
        dto.setAccountNo(ben.getAccountNo());
        dto.setIfscCode(ben.getIfscCode());
        dto.setVerified(ben.getVerified());
        dto.setStatus(ben.getStatus());
        return dto;
    }


    private DmtBeneficiaryFetchResponse.BeneficiaryData convertToDto(DmtBeneficiary b) {

        DmtBeneficiaryFetchResponse.BeneficiaryData dto =
                new DmtBeneficiaryFetchResponse.BeneficiaryData();

        dto.setBene_id(String.valueOf(b.getBeneId()));
        dto.setBankid(b.getBankId());
        dto.setBankname(b.getBankName());
        dto.setName(b.getName());
        dto.setAccno(b.getAccountNo());
        dto.setIfsc(b.getIfscCode());
        dto.setVerified(b.getVerified());
        dto.setBanktype(null); // if not available in DB
        dto.setPaytm(false);   // if not available in DB

        return dto;
    }

    public String generateJwtToken() {

        int random = getRandom();
        long timestamp = getTimestamp();

        String jwtKey =
                "UFMwMDY0MDQ5YTk5MTYxZmI4YjgxMjYyNzBlODY2NDAxMTg3NzIxMjE3NTczMzkxMDA=";

        System.out.println("JWT timestamp = " + timestamp);
        System.out.println("JWT reqid     = " + random);

        String token = Jwts.builder()
                .setIssuer("PSPRINT")
//                .setHeaderParam("typ", "JWT")
//                .setHeaderParam("alg", "HS256")
                .claim("timestamp", timestamp)
                .claim("iat", timestamp - 720)
                .claim("partnerId", "PS006404")
                .claim("product","WALLET")
                .claim("reqid", random)
                .signWith(SignatureAlgorithm.HS256,
                        jwtKey.getBytes(StandardCharsets.UTF_8))
                .compact();

        return token;
    }

    private static int getRandom() {
        int random = (int) Math.round(Math.random() * 1000000000);
        return random;
    }

    private static long getTimestamp() {
        long timestamp = Instant.now().getEpochSecond();
        return timestamp;
    }




}
