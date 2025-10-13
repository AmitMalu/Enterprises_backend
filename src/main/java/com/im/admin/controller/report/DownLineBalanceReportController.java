package com.im.admin.controller.report;

import com.im.admin.dto.DownlineBalanceDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.DownLineBalanceReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/downline-balance")
public class DownLineBalanceReportController {

    @Autowired
    private DownLineBalanceReportService downLineBalanceReportService;

    @GetMapping("/report/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<DownlineBalanceDto>>> getDownlineBalanceReport(
            @PathVariable String hierarchyId) {

        log.info("Fetching Downline Balance Report for hierarchyId: {}", hierarchyId);

        List<DownlineBalanceDto> report = downLineBalanceReportService.getDownlineBalanceReport(hierarchyId);

        if (report.isEmpty()) {
            return ResponseEntity.ok(new CommonResponse<>(404, "No downline records found", report));
        }

        return ResponseEntity.ok(new CommonResponse<>(200, "Fetched Downline Balance Report successfully", report));
    }

}
