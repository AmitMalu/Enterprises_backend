package com.im.admin.controller.report;


import com.im.admin.dto.SearchTransactionReportGroupedResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.SearchTransactionReportResponseDto;
import com.im.admin.service.reports.SearchTransactionReportService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/search-transaction")
@Slf4j
public class SearchTransactionReportController {

    @Autowired
    private SearchTransactionReportService searchTransactionReportService;

    @Autowired
    private CommonMethods commonMethods;

    @GetMapping("/view")
    public ResponseEntity<CommonResponse<SearchTransactionReportGroupedResponse>> searchTransaction(
            @RequestParam String transactionId,
            @RequestParam(required = false) String hierarchyId) {

        log.info("Received request to search transaction | transactionId={}, hierarchyId={}",
                transactionId, hierarchyId);

        String userType = (hierarchyId == null) ? "AD" : commonMethods.extractHierarchyType(hierarchyId);

        if ("AD".equalsIgnoreCase(userType) && hierarchyId != null) {
            log.warn("Admin provided hierarchyId which is not allowed | hierarchyId={}", hierarchyId);
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "Admin must not provide hierarchyId", null));
        } else if (!"AD".equalsIgnoreCase(userType) && hierarchyId.isBlank()) {
            log.warn("Non-admin user did not provide required hierarchyId");
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "hierarchyId is required for non-admin users", null));
        }

        try {
            SearchTransactionReportGroupedResponse result =
                    searchTransactionReportService.searchTransactionById(transactionId, hierarchyId);

            if (result.getTransactionDetails().isEmpty() &&
                    result.getWalletDetails().isEmpty()) {

                log.info("No transactions found for transactionId={}, hierarchyId={}", transactionId, hierarchyId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CommonResponse<>(404, "No transaction found", null));
            }

            log.info("Transactions fetched successfully | transactionId={}, hierarchyId={}, transactions={}, wallets={}",
                    transactionId, hierarchyId,
                    result.getTransactionDetails().size(),
                    result.getWalletDetails().size());

            return ResponseEntity.ok(new CommonResponse<>(200, "Transaction(s) fetched successfully", result));

        } catch (IllegalAccessException ex) {
            log.error("Access forbidden while searching transaction | transactionId={}, hierarchyId={}, error={}",
                    transactionId, hierarchyId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonResponse<>(403, ex.getMessage(), null));
        }
    }



}
