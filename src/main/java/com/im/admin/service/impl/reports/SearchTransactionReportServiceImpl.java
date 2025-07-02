package com.im.admin.service.impl.reports;

import com.im.admin.response.SearchTransactionReportResponseDto;
import com.im.admin.service.reports.AsyncSearchService;
import com.im.admin.service.reports.SearchTransactionReportService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@Slf4j
public class SearchTransactionReportServiceImpl implements SearchTransactionReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private AsyncSearchService asyncService;

    @Override
    public Page<SearchTransactionReportResponseDto> searchTransactionById(String txnId, String hierarchyId, Pageable pageable) throws IllegalAccessException {

        // Normalize transactionId to base form (remove prefix if needed)
        String normalizedTxnId = txnId.length() > 14 ? txnId.substring(txnId.length() - 17) : txnId;

        List<CompletableFuture<List<SearchTransactionReportResponseDto>>> futures = new ArrayList<>();
        String hierarchyType = (hierarchyId == null) ? "AD" : commonMethods.extractHierarchyType(hierarchyId);

        if ("AD".equalsIgnoreCase(hierarchyType)) {
            futures.add(asyncService.searchRetailer(normalizedTxnId));
            futures.add(asyncService.searchDistributor(normalizedTxnId));
            futures.add(asyncService.searchPartner(normalizedTxnId));
            futures.add(asyncService.searchSuperDistributor(normalizedTxnId));
            futures.add(asyncService.searchWhiteLabel(normalizedTxnId));
            futures.add(asyncService.searchEnterprise(normalizedTxnId));
            futures.add(asyncService.searchAdmin(normalizedTxnId));
        } else if ("WL-AD".equalsIgnoreCase(hierarchyType)) {
            futures.add(asyncService.searchRetailer(normalizedTxnId));
            futures.add(asyncService.searchDistributor(normalizedTxnId));
            futures.add(asyncService.searchPartner(normalizedTxnId));
            futures.add(asyncService.searchSuperDistributor(normalizedTxnId));
            futures.add(asyncService.searchWhiteLabel(normalizedTxnId));
            futures.add(asyncService.searchEnterprise(normalizedTxnId));
            futures.add(asyncService.searchAdmin(normalizedTxnId));
        } else {
            switch (hierarchyType) {
                case "RT" -> futures.add(asyncService.searchRetailerById(normalizedTxnId, hierarchyId));
                case "DT" -> futures.add(asyncService.searchDistributorById(normalizedTxnId, hierarchyId));
                case "PT" -> futures.add(asyncService.searchPartnerById(normalizedTxnId, hierarchyId));
                case "SD" -> futures.add(asyncService.searchSuperDistributorById(normalizedTxnId, hierarchyId));
                case "WL" -> futures.add(asyncService.searchWhiteLabelById(normalizedTxnId, hierarchyId));
                case "ET" -> futures.add(asyncService.searchEnterpriseById(normalizedTxnId, hierarchyId));
                case "AD" -> futures.add(asyncService.searchAdminById(normalizedTxnId, hierarchyId));
                default -> throw new IllegalAccessException("Unsupported hierarchy type.");
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SearchTransactionReportResponseDto> results = futures.stream()
                .flatMap(f -> {
                    try {
                        return f.get().stream();
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .sorted(Comparator.comparing(SearchTransactionReportResponseDto::getDate).reversed()
                        .thenComparing(SearchTransactionReportResponseDto::getTime).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        List<SearchTransactionReportResponseDto> pageContent = results.subList(start, end);

        return new PageImpl<>(pageContent, pageable, results.size());
    }

}
