package com.scs.admin.service.reports;

import com.scs.admin.dto.DebitCreditReportRequest;
import com.scs.admin.response.DebitCreditReportResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DebitCreditReportService {
    Page<DebitCreditReportResponse> getDebitCreditReport(DebitCreditReportRequest request, int page, int size);
}

