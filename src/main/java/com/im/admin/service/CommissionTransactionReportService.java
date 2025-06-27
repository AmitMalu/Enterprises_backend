package com.im.admin.service;

import com.im.admin.request.CommissionTransactionReportRequest;
import com.im.admin.response.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public interface CommissionTransactionReportService {

    CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(CommissionTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(CommissionTransactionReportRequest request);

}
