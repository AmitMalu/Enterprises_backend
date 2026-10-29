package com.scs.admin.service.impl.reports;

import com.scs.admin.dto.CommonSearchTransactionDetails;
import com.scs.admin.entities.RetailerPostpaidTransactionDetails;
import com.scs.admin.entities.RetailerPrepaidTransactionDetails;
import com.scs.admin.repositories.*;
import com.scs.admin.response.SearchTransactionReportResponseDto;
import com.scs.admin.service.reports.AsyncSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailerById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> retailerPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndRetailerId(txnId, networkIds),
                () -> retailerPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndRetailerIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributorById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> distributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndDistributorIdIn(txnId, networkIds),
                () -> distributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndDistributorIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartnerById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> partnerPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndPartnerIdIn(txnId, networkIds),
                () -> partnerPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndPartnerIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributorById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> superDistributorPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndSuperDistributorIdIn(txnId, networkIds),
                () -> superDistributorPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndSuperDistributorIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabelById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> whiteLabelPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndWhiteLabelIdIn(txnId, networkIds),
                () -> whiteLabelPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndWhiteLabelIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterpriseById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> enterprisesPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndEnterprisesIdIn(txnId, networkIds),
                () -> enterprisesPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndEnterprisesIdIn(txnId, networkIds)
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
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdminById(String txnId, List<String> networkIds) {
        return CompletableFuture.completedFuture(fetchTransactions(
                () -> adminPrepaidTransactionDetailsRepository.findTransactionsByTransactionIdAndAdminId(txnId, networkIds),
                () -> adminPostpaidTransactionDetailsRepository.findTransactionsByTransactionIdAndAdminId(txnId, networkIds)
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

        dto.setLatitude(txn.getLatitude());
        dto.setLongitude(txn.getLongitude());

        dto.setVendorName(txn.getVendorName());
        dto.setVendorTransactionId(txn.getVendorTransactionId());


        return dto;
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchTransactionsByHour(LocalDateTime start, LocalDateTime end) {
        List<SearchTransactionReportResponseDto> result = new ArrayList<>();

        LocalDate date = start.toLocalDate();
        int hour = start.getHour(); // only this hour is considered

        // Retailer
        result.addAll(fetchTransactions(
                () -> retailerPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> retailerPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // Distributor
        result.addAll(fetchTransactions(
                () -> distributorPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> distributorPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // Partner
        result.addAll(fetchTransactions(
                () -> partnerPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> partnerPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // SuperDistributor
        result.addAll(fetchTransactions(
                () -> superDistributorPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> superDistributorPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // WhiteLabel
        result.addAll(fetchTransactions(
                () -> whiteLabelPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> whiteLabelPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // Enterprises
        result.addAll(fetchTransactions(
                () -> enterprisesPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> enterprisesPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // Admin
        result.addAll(fetchTransactions(
                () -> adminPrepaidTransactionDetailsRepository.findByDateAndHour(date, hour),
                () -> adminPostpaidTransactionDetailsRepository.findByDateAndHour(date, hour)
        ));

        // At this point `result` contains merged Prepaid + Postpaid for all hierarchies
        return CompletableFuture.completedFuture(result);
    }


}
