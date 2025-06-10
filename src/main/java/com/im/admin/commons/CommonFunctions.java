package com.im.admin.commons;

import com.im.admin.dto.TokenResponseDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CommonFunctions {

    @Autowired
    private ImLogsRepository imLogsRepository;

    @Autowired
    private VendorReqResRepository vendorReqResRepository;

    @Autowired
    private HierarchyLogsRepository hierarchyLogsRepository;

    @Autowired
    private LoginLogsRepository loginLogsRepository;

    @Autowired
    private AuditLogsRepository auditLogsRepository;

    RestTemplate restTemplate = new RestTemplate();

    @Value("runpaisa.token.username")
    private String runpaisaUsername;

    @Value("runpaisa.token.password")
    private String runpaisaPassword;

    @Value("runpaisa.base.url.uat")
    private String baseUrlUAT;

    @Value("runpaisa.base.url.prod")
    private String baseUrlPROD;

    @Value("runpaisa.client.id")
    private String clientId;


    public ImLogs saveImLogs(String request, LocalDateTime imRequestDateTime, String ipAddress, String response,
                             LocalDateTime imResponseDateTime) {
        ImLogs imLogs = new ImLogs();
        imLogs.setRequest(request);
        imLogs.setRequestDateTime(imRequestDateTime);
        imLogs.setIpAddress(null);
        imLogs.setResponse(response);
        imLogs.setResponseDateTime(imResponseDateTime);
        imLogs = imLogsRepository.save(imLogs);
        return imLogs;
    }

    public VendorLogs saveVendorLogs(LocalDateTime vendorRequestDateTime, String request, String vendorId,
                                     LocalDateTime vendorResponseDateTime, String response, String requestIp) {
        VendorLogs vendorReqRes = new VendorLogs();
        vendorReqRes.setImVendorId(vendorId);
        vendorReqRes.setRequest(request);
        vendorReqRes.setRequestDateTime(vendorRequestDateTime);
        vendorReqRes.setResponse(response);
        vendorReqRes.setResponseDateTime(vendorResponseDateTime);
        vendorReqRes.setRequestIp(null);
        vendorReqRes = vendorReqResRepository.save(vendorReqRes);
        return vendorReqRes;
    }

    public HierarchyLogs saveHierarchyLogs(LocalDateTime HierarchyRequestDateTime, String request,String ipAddress,
                                        LocalDateTime HierarchyResponseDateTime, String response) {
        HierarchyLogs hierarchyLogs = new HierarchyLogs();
        hierarchyLogs.setRequest(request);
        hierarchyLogs.setRequestDateTime(HierarchyRequestDateTime);
        hierarchyLogs.setResponse(response);
        hierarchyLogs.setResponseDateTime(HierarchyResponseDateTime);
        hierarchyLogs.setIpAddress(null);
        hierarchyLogs = hierarchyLogsRepository.save(hierarchyLogs);
        return hierarchyLogs;
    }

    public LoginLogs saveLoginLogs(String request,LocalDateTime LoginRequestDateTime,String response,
                                   LocalDateTime LoginResponseDateTime, String requestIp) {
        LoginLogs loginlogs = new LoginLogs();
        loginlogs.setRequest(request);
        loginlogs.setRequestDateTime(LoginRequestDateTime);
        loginlogs.setResponse(response);
        loginlogs.setResponseDateTime(LoginResponseDateTime);
        loginlogs.setIpAddress(null);
        loginlogs = loginLogsRepository.save(loginlogs);
        return loginlogs;
    }

    public AuditLogs saveAuditLogs(String request, LocalDateTime imRequestDateTime, String ipAddress, String response,
                             LocalDateTime imResponseDateTime) {
        AuditLogs auditogs = new AuditLogs();
        auditogs.setRequest(request);
        auditogs.setRequestDateTime(imRequestDateTime);
        auditogs.setIpAddress(null);
        auditogs.setResponse(response);
        auditogs.setResponseDateTime(imResponseDateTime);
        auditogs = auditLogsRepository.save(auditogs);
        return auditogs;
    }


    public TokenResponseDTO generateToken() {
        Map<String, String> requestPayload = new HashMap<>();

        requestPayload.put("username", this.runpaisaUsername);
        requestPayload.put("password", this.runpaisaPassword);
        requestPayload.put("client_id ", this.clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity(requestPayload, headers);
        ResponseEntity<TokenResponseDTO> response = this.restTemplate.postForEntity(this.baseUrlUAT + "/token",
                requestEntity, TokenResponseDTO.class);

        TokenResponseDTO bearerTokenDto = response.getBody();
        log.info("Bearer token: {}", bearerTokenDto);

        return bearerTokenDto;
    }

}
