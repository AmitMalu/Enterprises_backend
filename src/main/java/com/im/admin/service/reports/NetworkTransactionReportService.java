package com.im.admin.service.reports;

import com.im.admin.request.reports.NetworkTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.NetworkTransactionReportResponse;
import org.springframework.data.domain.Page;

public interface NetworkTransactionReportService {

    CommonResponse<Page<NetworkTransactionReportResponse>> viewNetworkTransactions(NetworkTransactionReportRequest request);

}
