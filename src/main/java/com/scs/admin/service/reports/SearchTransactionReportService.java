package com.scs.admin.service.reports;

import com.scs.admin.dto.SearchTransactionReportGroupedResponse;
import com.scs.admin.response.SearchTransactionReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchTransactionReportService {

    SearchTransactionReportGroupedResponse searchTransactionById(
            String txnId, String hierarchyId
    ) throws IllegalAccessException;

}
