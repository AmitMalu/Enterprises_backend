package com.im.admin.commons;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class CommonFunctions {

    @Autowired
    private ImLogsRepository imLogsRepository;

    @Autowired
    private VendorLogsRepository vendorReqResRepository;

    @Autowired
    private HierarchyLogsRepository hierarchyLogsRepository;

    @Autowired
    private LoginLogsRepository loginLogsRepository;

    @Autowired
    private AuditLogsRepository auditLogsRepository;

    @Autowired
    private CallbackEventLogRepository callbackEventLogRepository;

    @Autowired
    private SmsLogsRepository smsLogRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;


    public void saveImLogs(String request, LocalDateTime imRequestDateTime, String ipAddress, String response,
                           LocalDateTime imResponseDateTime) {
        ImLogs imLogs = new ImLogs();
        imLogs.setRequest(request);
        imLogs.setRequestDateTime(imRequestDateTime);
        imLogs.setIpAddress(ipAddress);
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
        vendorReqRes.setRequestIp(requestIp);
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
        loginlogs.setIpAddress(requestIp);
        loginlogs = loginLogsRepository.save(loginlogs);
    }

    public AuditLogs saveAuditLogs(String request, LocalDateTime imRequestDateTime, String ipAddress, String response,
                                   LocalDateTime imResponseDateTime) {
        AuditLogs auditogs = new AuditLogs();
        auditogs.setRequest(request);
        auditogs.setRequestDateTime(imRequestDateTime);
        auditogs.setIpAddress(ipAddress);
        auditogs.setResponse(response);
        auditogs.setResponseDateTime(imResponseDateTime);
        auditogs = auditLogsRepository.save(auditogs);
        return auditogs;
    }


//    public void saveCallbackEventLog(String request, LocalDateTime requestDateTime, String ipAddress,
//                                     String response, LocalDateTime responseDateTime) {
//
//        CallbackEventLog callbackEventLog = new CallbackEventLog();
//        callbackEventLog.setRequest(request);
//        callbackEventLog.setRequestDateTime(requestDateTime);
//        callbackEventLog.setIpAddress(ipAddress);
//        callbackEventLog.setResponse(response);
//        callbackEventLog.setResponseDateTime(responseDateTime);
//        callbackEventLogRepository.save(callbackEventLog);
//    }

    public void saveCallbackEventLog(Map<String, String> payload,
                                     LocalDateTime requestDateTime,
                                     String ipAddress,
                                     String response,
                                     LocalDateTime responseDateTime) {

        CallbackEventLog callbackEventLog = new CallbackEventLog();

        callbackEventLog.setRequest(payload.toString());
        callbackEventLog.setRequestDateTime(requestDateTime);
        callbackEventLog.setIpAddress(ipAddress);
        callbackEventLog.setResponse(response);
        callbackEventLog.setResponseDateTime(responseDateTime);


        String orderId = extractOrderId(payload);

        callbackEventLog.setOrderId(orderId);

        callbackEventLogRepository.save(callbackEventLog);
    }

    private String extractOrderId(Map<String, String> payload) {

        for (Map.Entry<String, String> entry : payload.entrySet()) {

            String key = entry.getKey();

            if ("ORDER_ID".equalsIgnoreCase(key)
                    || "ORDERID".equalsIgnoreCase(key)
                    || "ORDER-ID".equalsIgnoreCase(key)) {

                return entry.getValue();
            }
        }

        return null; // if not found
    }




    public SmsLog saveSmsLogs(LocalDateTime smsRequestDateTime, String request, LocalDateTime smsResponseDateTime, String response, String ipAddress) {
        SmsLog smsLog = new SmsLog();
        smsLog.setRequest(request);
        smsLog.setRequestDateTime(smsRequestDateTime);
        smsLog.setResponse(response);
        smsLog.setResponseDateTime(smsResponseDateTime);
        smsLog.setIpAddress(null);
        smsLog = smsLogRepository.save(smsLog);
        return smsLog;
    }


    public EmailLog saveEmailLogs(LocalDateTime smsRequestDateTime, String request, LocalDateTime smsResponseDateTime, String response, String ipAddress) {
        EmailLog emailLog = new EmailLog();
        emailLog.setRequest(request);
        emailLog.setRequestDateTime(smsRequestDateTime);
        emailLog.setResponse(response);
        emailLog.setResponseDateTime(smsResponseDateTime);
        emailLog.setIpAddress(null);
        emailLog = emailLogRepository.save(emailLog);
        return emailLog;
    }

}
