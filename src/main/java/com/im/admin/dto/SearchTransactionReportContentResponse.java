package com.im.admin.dto;

import com.im.admin.response.SearchTransactionReportResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchTransactionReportContentResponse {
    private List<SearchTransactionReportResponseDto> transactionDetails;
    private List<SearchTransactionReportResponseDto> walletDetails;
}
