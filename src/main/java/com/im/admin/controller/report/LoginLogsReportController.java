package com.im.admin.controller.report;

import com.im.admin.dto.LoginLogsResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.LoginLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class LoginLogsReportController {

    @Autowired
    private LoginLogsReportService loginLogsReportService;

    @PostMapping("/login-logs-json")
    public ResponseEntity<?> getLoginLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<LoginLogsResponseDTO> data = loginLogsReportService.getFilteredLogs(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (data.isEmpty()) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", 200);
            responseMap.put("message", "No records found");
            return ResponseEntity.ok(responseMap);
        }

//        return ResponseEntity.ok(data);

        return ResponseEntity
                .ok(new CommonResponse(201, "Success", data));
    }


    @PostMapping("/login-logs-excel")
    public ResponseEntity<?> downloadLoginLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ByteArrayResource resource = loginLogsReportService.exportLogsToCSV(
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (resource == null) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", 200);
            responseMap.put("message", "No records found");
            return ResponseEntity.ok(responseMap);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=LoginLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

}
