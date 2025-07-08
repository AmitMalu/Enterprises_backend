package com.im.admin.controller.report;


import com.im.admin.request.reports.NetworkTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.NetworkTransactionReportResponse;
import com.im.admin.service.reports.NetworkTransactionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/network-transaction")
@Slf4j
public class NetworkTransactionReportController {

    @Autowired
    private NetworkTransactionReportService networkTransactionReportService;


    @PostMapping("/view")
    public ResponseEntity<CommonResponse<Page<NetworkTransactionReportResponse>>> viewNetworkTransactions(
            @RequestBody NetworkTransactionReportRequest request) {

        CommonResponse<Page<NetworkTransactionReportResponse>> response = networkTransactionReportService.viewNetworkTransactions(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

}
