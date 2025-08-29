package com.im.admin.commons;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

    @Autowired
    private CallbackEventLogRepository callbackEventLogRepository;


    public void saveImLogs(String request, LocalDateTime imRequestDateTime, String ipAddress, String response,
                           LocalDateTime imResponseDateTime) {
        ImLogs imLogs = new ImLogs();
        imLogs.setRequest(request);
        imLogs.setRequestDateTime(imRequestDateTime);
        imLogs.setIpAddress(null);
        imLogs.setResponse(response);
        imLogs.setResponseDateTime(imResponseDateTime);
        imLogs = imLogsRepository.save(imLogs);
    }

    public void saveVendorLogs(LocalDateTime vendorRequestDateTime, String request, String vendorId,
                               LocalDateTime vendorResponseDateTime, String response, String requestIp) {
        VendorLogs vendorReqRes = new VendorLogs();
        vendorReqRes.setImVendorId(vendorId);
        vendorReqRes.setRequest(request);
        vendorReqRes.setRequestDateTime(vendorRequestDateTime);
        vendorReqRes.setResponse(response);
        vendorReqRes.setResponseDateTime(vendorResponseDateTime);
        vendorReqRes.setRequestIp(null);
        vendorReqRes = vendorReqResRepository.save(vendorReqRes);
    }

    public void saveHierarchyLogs(LocalDateTime HierarchyRequestDateTime, String request, String ipAddress,
                                  LocalDateTime HierarchyResponseDateTime, String response) {
        HierarchyLogs hierarchyLogs = new HierarchyLogs();
        hierarchyLogs.setRequest(request);
        hierarchyLogs.setRequestDateTime(HierarchyRequestDateTime);
        hierarchyLogs.setResponse(response);
        hierarchyLogs.setResponseDateTime(HierarchyResponseDateTime);
        hierarchyLogs.setIpAddress(ipAddress);
        hierarchyLogs = hierarchyLogsRepository.save(hierarchyLogs);
    }

    public void saveLoginLogs(String request, LocalDateTime LoginRequestDateTime, String response,
                              LocalDateTime LoginResponseDateTime, String requestIp) {
        LoginLogs loginlogs = new LoginLogs();
        loginlogs.setRequest(request);
        loginlogs.setRequestDateTime(LoginRequestDateTime);
        loginlogs.setResponse(response);
        loginlogs.setResponseDateTime(LoginResponseDateTime);
        loginlogs.setIpAddress(null);
        loginlogs = loginLogsRepository.save(loginlogs);
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


    public void saveCallbackEventLog(String request, LocalDateTime requestDateTime, String ipAddress,
                                     String response, LocalDateTime responseDateTime) {

        CallbackEventLog callbackEventLog = new CallbackEventLog();
        callbackEventLog.setRequest(request);
        callbackEventLog.setRequestDateTime(requestDateTime);
        callbackEventLog.setIpAddress(ipAddress);
        callbackEventLog.setResponse(response);
        callbackEventLog.setResponseDateTime(responseDateTime);
        callbackEventLogRepository.save(callbackEventLog);
    }

}
