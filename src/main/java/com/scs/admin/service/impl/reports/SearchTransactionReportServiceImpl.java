package com.scs.admin.service.impl.reports;

import com.scs.admin.dto.SearchTransactionReportContentResponse;
import com.scs.admin.dto.SearchTransactionReportGroupedResponse;
import com.scs.admin.entities.UserDetails;
import com.scs.admin.repositories.UserDetailsRepository;
import com.scs.admin.response.SearchTransactionReportResponseDto;
import com.scs.admin.service.reports.AsyncSearchService;
import com.scs.admin.service.reports.SearchTransactionReportService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import com.scs.admin.util.TransactionPrefixUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
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

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Override
    public SearchTransactionReportGroupedResponse searchTransactionById(
            String txnId, String hierarchyId) throws IllegalAccessException {

        String normalizedTxnId = txnId.length() > 14 ? txnId.substring(txnId.length() - 17) : txnId;

        List<CompletableFuture<List<SearchTransactionReportResponseDto>>> futures = new ArrayList<>();
//        String hierarchyType = (hierarchyId == null) ? "AD" : commonMethods.extractHierarchyType(hierarchyId);

        String loggedInHierarchyId = tokenUtil.extractHierarchyId();
        String hierarchyType = commonMethods.extractHierarchyType(loggedInHierarchyId);

        List<String> networkIds = getRetailersUnderHierarchy(loggedInHierarchyId);


        if ("AD".equalsIgnoreCase(hierarchyType) || "WL-AD".equalsIgnoreCase(hierarchyType)) {
            futures.add(asyncService.searchRetailer(normalizedTxnId));
            futures.add(asyncService.searchDistributor(normalizedTxnId));
            futures.add(asyncService.searchPartner(normalizedTxnId));
            futures.add(asyncService.searchSuperDistributor(normalizedTxnId));
            futures.add(asyncService.searchWhiteLabel(normalizedTxnId));
            futures.add(asyncService.searchEnterprise(normalizedTxnId));
            futures.add(asyncService.searchAdmin(normalizedTxnId));
        } else {
            switch (hierarchyType) {
                case "RT" -> futures.add(asyncService.searchRetailerById(normalizedTxnId, networkIds));
                case "DT" -> futures.add(asyncService.searchDistributorById(normalizedTxnId, networkIds));
                case "PT" -> futures.add(asyncService.searchPartnerById(normalizedTxnId, networkIds));
                case "SD" -> futures.add(asyncService.searchSuperDistributorById(normalizedTxnId, networkIds));
                case "WL" -> futures.add(asyncService.searchWhiteLabelById(normalizedTxnId, networkIds));
                case "ET" -> futures.add(asyncService.searchEnterpriseById(normalizedTxnId, networkIds));
                case "AD" -> futures.add(asyncService.searchAdminById(normalizedTxnId, networkIds));
                default -> throw new IllegalAccessException("Unsupported hierarchy type.");
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SearchTransactionReportResponseDto> allResults = futures.stream()
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

        List<SearchTransactionReportResponseDto> transactionDetailsList = allResults.stream()
                .filter(dto -> TransactionPrefixUtil.isTransaction(dto.getTransactionId()))
                .toList();

        List<SearchTransactionReportResponseDto> walletDetailsList = allResults.stream()
                .filter(dto -> TransactionPrefixUtil.isWallet(dto.getTransactionId()))
                .toList();

        return new SearchTransactionReportGroupedResponse(transactionDetailsList, walletDetailsList);
    }



    public List<String> getRetailersUnderHierarchy(String hierarchyId) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

//        if ("RT".equals(type) || "RU".equals(type)) {
//            return List.of(hierarchyId);
//        }
        if ("RU".equals(type)) {
            return List.of(hierarchyId);
        }

        List<String> resultIds = new ArrayList<>();

        // Add itself
        resultIds.add(hierarchyId);

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        log.info("All children IDs fetched: {}",
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList());


        resultIds.addAll(
                allChildren.stream()
                        .filter(user ->
                                "RT".equalsIgnoreCase(user.getHierarchyCode()) ||
                                        "RU".equalsIgnoreCase(user.getHierarchyCode())
                        )
                        .map(UserDetails::getHierarchyId)
                        .toList()
        );

        return resultIds;
    }


    private void fetchChildrenRecursively(
            String parentId,
            List<UserDetails> result
    ) {

        List<UserDetails> children =
                userDetailsRepository.findByParentAccount(parentId);

        for (UserDetails child : children) {
            result.add(child);
            fetchChildrenRecursively(child.getHierarchyId(), result);
        }
    }
}
