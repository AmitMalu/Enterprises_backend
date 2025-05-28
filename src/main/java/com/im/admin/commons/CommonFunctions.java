package com.im.admin.commons;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
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

    public VendorReqRes saveVendorLogs(LocalDateTime vendorRequestDateTime, String request, String vendorId,
                                       LocalDateTime vendorResponseDateTime, String response, String requestIp) {
        VendorReqRes vendorReqRes = new VendorReqRes();
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


}
