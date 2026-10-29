package com.scs.admin.service.reports;

import com.scs.admin.dto.DownlineBalanceDto;
import com.scs.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DownLineBalanceReportService {
    CommonResponse<Page<DownlineBalanceDto>> getDownlineBalanceReport(
            String hierarchyId,
            String userId,
            String name,
            String parentAccount,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minPrepaidBalance,
            BigDecimal maxPrepaidBalance,
            BigDecimal minPostpaidBalance,
            BigDecimal maxPostpaidBalance,
            int page,
            int size);

}

