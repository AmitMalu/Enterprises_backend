package com.scs.admin.controller.report;

import com.scs.admin.dto.DebitCreditReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.DebitCreditReportResponse;
import com.scs.admin.service.reports.DebitCreditReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debit-credit")
public class DebitCreditReportController {

    @Autowired
    private DebitCreditReportService debitCreditReportService;

    @PostMapping("/report")
    public ResponseEntity<CommonResponse<Page<DebitCreditReportResponse>>> getDebitCreditReport(
            @RequestBody DebitCreditReportRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DebitCreditReportResponse> pageResult = debitCreditReportService.getDebitCreditReport(request, page, size);

        return ResponseEntity.ok(
                CommonResponse.success(pageResult, "Report fetched successfully")
        );
    }


}
