package com.scs.admin.service.reports;

import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.request.reports.NetworkTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.NetworkTransactionReportResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface NetworkTransactionReportService {

    CommonResponse<Page<NetworkTransactionReportResponse>> viewNetworkTransactions(NetworkTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(NetworkTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(NetworkTransactionReportRequest request) throws IOException;

}
