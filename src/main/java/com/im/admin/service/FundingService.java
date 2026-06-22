package com.im.admin.service;

import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.dto.RetailerTransactionResponseDto;
import com.im.admin.dto.RetailerTransactionResponseDtoCom;
import com.im.admin.dto.SettlementPendingRequestDto;
import com.im.admin.request.HierarchyWalletReportRequest;
import com.im.admin.response.BulkFundResponse;
import com.im.admin.response.HierarchyWalletReportDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FundingService {

    String fundsCreditDebit(FundCreditDebitDto dto);

    void downloadSampleFile(HttpServletResponse response) throws IOException;

    List<BulkFundResponse> processBulkFile(MultipartFile file, Double latitude, Double longitude);

    Page<RetailerTransactionResponseDto> getSettlementPendingTransactions(SettlementPendingRequestDto request, int page, int size);

    Page<RetailerTransactionResponseDto> getSettlementPendingTransactionsUpdated(SettlementPendingRequestDto request, int page, int size);

    Page<RetailerTransactionResponseDto> getTransactionReport(SettlementPendingRequestDto request, int page, int size);

    Page<RetailerTransactionResponseDtoCom> getTransactionReportComWise(SettlementPendingRequestDto request, int page, int size);

    List<String> getRetailersUnderHierarchy(String hierarchyId);


    Page<HierarchyWalletReportDto> getReport(HierarchyWalletReportRequest request);
}
