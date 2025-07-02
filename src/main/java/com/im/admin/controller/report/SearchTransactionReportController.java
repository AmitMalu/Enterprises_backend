package com.im.admin.controller.report;


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

    @GetMapping("/show-records")
    public ResponseEntity<CommonResponse<Page<SearchTransactionReportResponseDto>>> searchTransaction(
            @RequestParam String transactionId,
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userType = (hierarchyId == null) ? "AD" : commonMethods.extractHierarchyType(hierarchyId);

        if ("AD".equalsIgnoreCase(userType) && hierarchyId != null) {
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "Admin must not provide hierarchyId", null));
        } else if (!"AD".equalsIgnoreCase(userType) && (hierarchyId == null || hierarchyId.isBlank())) {
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "hierarchyId is required for non-admin users", null));
        }

        Pageable pageable = PageRequest.of(page, size);

        try {
            Page<SearchTransactionReportResponseDto> result =
                    searchTransactionReportService.searchTransactionById(transactionId, hierarchyId, pageable);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CommonResponse<>(
                                404,
                                "No transaction found",
                                new PageImpl<>(new ArrayList<>(), pageable, 0)
                        ));

            }

            return ResponseEntity.ok(new CommonResponse<>(200, "Transaction(s) fetched successfully", result));

        } catch (IllegalAccessException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonResponse<>(403, ex.getMessage(), new PageImpl<>(new ArrayList<>(), pageable, 0)));
        }
    }

}
