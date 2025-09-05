package com.im.admin.service.impl.reports;

import com.im.admin.dto.CommonSearchTransactionDetails;
import com.im.admin.entities.RetailerPostpaidTransactionDetails;
import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import com.im.admin.repositories.*;
import com.im.admin.response.SearchTransactionReportResponseDto;
import com.im.admin.service.reports.AsyncSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AsyncSearchServiceImpl implements AsyncSearchService {

    @Autowired private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;
    @Autowired private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired private DistributorPrepaidTransactionDetailsRepository distributorPrepaidTransactionDetailsRepository;
    @Autowired private DistributorPostpaidTransactionDetailsRepository distributorPostpaidTransactionDetailsRepository;

    @Autowired private PartnerPrepaidTransactionDetailsRepository partnerPrepaidTransactionDetailsRepository;
    @Autowired private PartnerPostpaidTransactionDetailsRepository partnerPostpaidTransactionDetailsRepository;

    @Autowired private SuperDistributorPrepaidTransactionDetailsRepository superDistributorPrepaidTransactionDetailsRepository;
    @Autowired private SuperDistributorPostpaidTransactionDetailsRepository superDistributorPostpaidTransactionDetailsRepository;

    @Autowired private WhiteLabelPrepaidTransactionDetailsRepository whiteLabelPrepaidTransactionDetailsRepository;
    @Autowired private WhiteLabelPostpaidTransactionDetailsRepository whiteLabelPostpaidTransactionDetailsRepository;

    @Autowired private EnterprisesPrepaidTransactionDetailsRepository enterprisesPrepaidTransactionDetailsRepository;
    @Autowired private EnterprisesPostpaidTransactionDetailsRepository enterprisesPostpaidTransactionDetailsRepository;

    @Autowired private AdminPrepaidTransactionDetailsRepository adminPrepaidTransactionDetailsRepository;
    @Autowired private AdminPostpaidTransactionDetailsRepository adminPostpaidTransactionDetailsRepository;

    // -------- Retailer --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailerById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> retailerPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndRetailerId(txnId, hierarchyId),
                () -> retailerPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndRetailerId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailer(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> retailerPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> retailerPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- Distributor --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributorById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> distributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndDistributorId(txnId, hierarchyId),
                () -> distributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndDistributorId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributor(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> distributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> distributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }


    // -------- Partner --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartnerById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> partnerPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndPartnerId(txnId, hierarchyId),
                () -> partnerPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndPartnerId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartner(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> partnerPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> partnerPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- Super Distributor --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributorById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> superDistributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndSuperDistributorId(txnId, hierarchyId),
                () -> superDistributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndSuperDistributorId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributor(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> superDistributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> superDistributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- White Label --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabelById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> whiteLabelPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndWhiteLabelId(txnId, hierarchyId),
                () -> whiteLabelPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndWhiteLabelId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabel(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> whiteLabelPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> whiteLabelPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- Enterprise --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterpriseById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> enterprisesPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndEnterprisesId(txnId, hierarchyId),
                () -> enterprisesPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndEnterprisesId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterprise(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> enterprisesPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> enterprisesPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- Admin --------
    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdminById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> adminPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndAdminId(txnId, hierarchyId),
                () -> adminPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndAdminId(txnId, hierarchyId)
        ));
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdmin(String txnId) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> adminPrepaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId),
                () -> adminPostpaidTransactionDetailsRepository.findTransactionsByTransactionId(txnId)
        ));
    }

    // -------- Helper --------
    private List<SearchTransactionReportResponseDto> fetchTransactions(
            Supplier<List<? extends CommonSearchTransactionDetails>> prepaidSupplier,
            Supplier<List<? extends CommonSearchTransactionDetails>> postpaidSupplier) {

        List<SearchTransactionReportResponseDto> result = new ArrayList<>();

        prepaidSupplier.get().forEach(txn -> result.add(mapToDto(txn, "Prepaid")));
        postpaidSupplier.get().forEach(txn -> result.add(mapToDto(txn, "Postpaid")));

        return result;
    }

    private SearchTransactionReportResponseDto mapToDto(CommonSearchTransactionDetails txn, String walletType) {
        SearchTransactionReportResponseDto dto = new SearchTransactionReportResponseDto();
        dto.setTransactionId(txn.getTransactionId());
        dto.setHierarchyId(txn.getHierarchyId());
        dto.setWalletType(walletType);
        dto.setBalBefore(txn.getBalBeforeTran());
        dto.setAmount(txn.getAmount());
        dto.setBalAfter(txn.getBalAfterTran());
        dto.setSubProductName(txn.getSubProductName());
        dto.setNarration(txn.getNarration());
        dto.setStatus(txn.getTranStatus());
        dto.setDate(txn.getDateOfTransaction());
        dto.setTime(txn.getTimeOfTransaction());
        return dto;
    }

}
