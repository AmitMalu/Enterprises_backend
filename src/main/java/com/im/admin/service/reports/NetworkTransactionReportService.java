package com.im.admin.service.reports;

import com.im.admin.request.reports.MyTransactionReportRequest;
import com.im.admin.request.reports.NetworkTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.NetworkTransactionReportResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface NetworkTransactionReportService {

    CommonResponse<Page<NetworkTransactionReportResponse>> viewNetworkTransactions(NetworkTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(NetworkTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(NetworkTransactionReportRequest request) throws IOException;

}
