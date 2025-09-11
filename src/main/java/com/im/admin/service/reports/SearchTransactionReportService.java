package com.im.admin.service.reports;

import com.im.admin.dto.SearchTransactionReportGroupedResponse;
import com.im.admin.response.SearchTransactionReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchTransactionReportService {

    SearchTransactionReportGroupedResponse searchTransactionById(
            String txnId, String hierarchyId
    ) throws IllegalAccessException;

}
