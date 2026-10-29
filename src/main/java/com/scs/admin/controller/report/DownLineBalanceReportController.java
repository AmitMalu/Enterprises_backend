package com.scs.admin.controller.report;

import com.scs.admin.dto.DownlineBalanceDto;
import com.scs.admin.dto.DownlineBalanceRequestDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.reports.DownLineBalanceReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @PostMapping("/report/{hierarchyId}")
    public ResponseEntity<CommonResponse<Page<DownlineBalanceDto>>> getDownlineBalanceReport(
            @PathVariable String hierarchyId,
            @RequestBody DownlineBalanceRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching Downline Balance Report for hierarchyId: {} | page: {} | size: {}",
                hierarchyId, page, size);

        var paginatedResponse = downLineBalanceReportService.getDownlineBalanceReport(
                hierarchyId,
                request.getUserId(),
                request.getName(),
                request.getParentAccount(),
                request.getFromDate(),
                request.getToDate(),
                request.getMinPrepaidBalance(),
                request.getMaxPrepaidBalance(),
                request.getMinPostpaidBalance(),
                request.getMaxPostpaidBalance(),
                page,
                size
        );

        return ResponseEntity.ok(paginatedResponse);
    }
}
