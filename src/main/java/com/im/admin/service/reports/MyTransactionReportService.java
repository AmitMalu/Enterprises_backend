package com.im.admin.service.reports;


import com.im.admin.request.reports.MyTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.MyTransactionReportResponse;
import org.springframework.data.domain.Page;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface MyTransactionReportService {

    CommonResponse<Page<MyTransactionReportResponse>> getReport(MyTransactionReportRequest request,int page,
                                                                int size);

    ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException;

    ByteArrayInputStream downloadCsv(MyTransactionReportRequest request) throws IOException;

}
