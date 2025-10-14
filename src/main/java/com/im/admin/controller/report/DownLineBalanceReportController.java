package com.im.admin.controller.report;

import com.im.admin.dto.DownlineBalanceDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.DownLineBalanceReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/downline-balance")
public class DownLineBalanceReportController {

    @Autowired
    private DownLineBalanceReportService downLineBalanceReportService;

    @GetMapping("/report/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<DownlineBalanceDto>>> getDownlineBalanceReport(
            @PathVariable String hierarchyId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String parentAccount,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minPrepaidBalance,
            @RequestParam(required = false) BigDecimal maxPrepaidBalance,
            @RequestParam(required = false) BigDecimal minPostpaidBalance,
            @RequestParam(required = false) BigDecimal maxPostpaidBalance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching Downline Balance Report for hierarchyId: {} | page: {} | size: {}", hierarchyId, page, size);

        var paginatedResponse = downLineBalanceReportService.getDownlineBalanceReport(
                hierarchyId, userId, name, parentAccount, fromDate, toDate,
                minPrepaidBalance, maxPrepaidBalance, minPostpaidBalance, maxPostpaidBalance,
                page, size);

        return ResponseEntity.ok(paginatedResponse);
    }

}
