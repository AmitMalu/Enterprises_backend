package com.im.admin.service.reports;

import com.im.admin.response.SearchTransactionReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchTransactionReportService {

    Page<SearchTransactionReportResponseDto> searchTransactionById(String txnId, String hierarchyId, Pageable pageable) throws IllegalAccessException;

}
