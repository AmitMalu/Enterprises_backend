package com.im.admin.service.reports;

import com.im.admin.dto.DebitCreditReportRequest;
import com.im.admin.response.DebitCreditReportResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DebitCreditReportService {
    Page<DebitCreditReportResponse> getDebitCreditReport(DebitCreditReportRequest request, int page, int size);
}

