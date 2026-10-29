package com.scs.admin.dto;

import com.scs.admin.response.SearchTransactionReportResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchTransactionReportGroupedResponse {

    private List<SearchTransactionReportResponseDto> transactionDetails;
    private List<SearchTransactionReportResponseDto> walletDetails;
}
