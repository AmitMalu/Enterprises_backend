package com.im.admin.service;


import com.im.admin.request.MyTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.MyTransactionReportResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface MyTransactionReportService {

    CommonResponse<Page<MyTransactionReportResponse>> getReport(MyTransactionReportRequest request);

    ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(MyTransactionReportRequest request) throws IOException;

}
