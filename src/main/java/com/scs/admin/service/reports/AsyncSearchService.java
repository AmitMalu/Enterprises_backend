package com.scs.admin.service.reports;

import com.scs.admin.response.SearchTransactionReportResponseDto;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncSearchService {

    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailer(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributor(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartner(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributor(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabel(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterprise(String txnId);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdmin(String txnId);

    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailerById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributorById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartnerById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributorById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabelById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterpriseById(String txnId, List<String> networkIds);
    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdminById(String txnId, List<String> networkIds);

    @Async CompletableFuture<List<SearchTransactionReportResponseDto>> searchTransactionsByHour(LocalDateTime start, LocalDateTime end);

}
