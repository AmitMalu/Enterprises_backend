package com.scs.admin.service.reports;

import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.MyTransactionReportResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for Transaction Report — returns only TXN% transactions.
 * Same contract as MyTransactionReportService, filtered to TXN only.
 */
public interface TransactionReportService {

    CommonResponse<Page<MyTransactionReportResponse>> getReport(
            MyTransactionReportRequest request, int page, int size);

    ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException;
}
