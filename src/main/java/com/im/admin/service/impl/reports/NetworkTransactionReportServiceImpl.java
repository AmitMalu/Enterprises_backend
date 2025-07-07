package com.im.admin.service.impl.reports;


import com.im.admin.request.reports.NetworkTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.NetworkTransactionReportResponse;
import com.im.admin.service.reports.NetworkTransactionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@Slf4j
public class NetworkTransactionReportServiceImpl implements NetworkTransactionReportService {


    @Override
    public CommonResponse<Page<NetworkTransactionReportResponse>> viewNetworkTransactions(NetworkTransactionReportRequest request) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("transactionDate").descending());

        LocalDate from = StringUtils.hasText(request.getFromDate()) ? LocalDate.parse(request.getFromDate()) : null;
        LocalDate to = StringUtils.hasText(request.getToDate()) ? LocalDate.parse(request.getToDate()) : null;
        return null;
    }
}
