package com.im.admin.service.reports;

import com.im.admin.request.reports.CommissionTransactionReportRequest;
import com.im.admin.response.reports.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface CommissionChargeTransactionReportService {

    CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(CommissionTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(CommissionTransactionReportRequest request);

}
