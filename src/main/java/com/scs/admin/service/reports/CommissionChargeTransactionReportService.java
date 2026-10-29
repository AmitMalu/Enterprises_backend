package com.scs.admin.service.reports;

import com.scs.admin.request.reports.CommissionTransactionReportRequest;
import com.scs.admin.response.reports.CommissionTransactionReportResponse;
import com.scs.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface CommissionChargeTransactionReportService {

    CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(CommissionTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(CommissionTransactionReportRequest request);

}
