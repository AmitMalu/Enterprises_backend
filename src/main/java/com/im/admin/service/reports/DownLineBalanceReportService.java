package com.im.admin.service.reports;

import com.im.admin.dto.DownlineBalanceDto;

import java.util.List;

public interface DownLineBalanceReportService {
    List<DownlineBalanceDto> getDownlineBalanceReport(String hierarchyId);
}
