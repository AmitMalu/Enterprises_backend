package com.im.admin.service.impl.reports;

import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import com.im.admin.entities.RetailerPostpaidTransactionDetails;
import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import com.im.admin.repositories.*;
import com.im.admin.response.SearchTransactionReportResponseDto;
import com.im.admin.service.reports.AsyncSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    @Autowired private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired private EnterprisesPrepaidTransactionDetailsRepository enterprisesPrepaidTransactionDetailsRepository;

    @Autowired private EnterprisesPostpaidTransactionDetailsRepository enterprisesPostpaidTransactionDetailsRepository;

    @Autowired private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

    @Autowired private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailer(String txnId) {
        // Replace this stub with actual DB logic
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributor(String txnId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartner(String txnId) {
        // Replace this stub with actual DB logic
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributor(String txnId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabel(String txnId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterprise(String txnId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdmin(String txnId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    // Repeat for all other methods...

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchRetailerById(String txnId, String hierarchyId) {
        List<SearchTransactionReportResponseDto> result = new ArrayList<>();

//        //  Retailer Prepaid Transactions
//        List<RetailerPrepaidTransactionDetails> prepaidTxns =
//                retailerPrepaidTransactionDetailsRepository.findByTransactionIdAndHierarchyId(txnId, hierarchyId);
//        for (RetailerPrepaidTransactionDetails txn : prepaidTxns) {
//            result.add(mapToDto(txn, "Prepaid"));
//        }
//
//        //  Retailer Postpaid Transactions
//        List<RetailerPostpaidTransactionDetails> postpaidTxns =
//                retailerPostpaidTransactionDetailsRepository.findByTransactionIdAndHierarchyId(txnId, hierarchyId);
//        for (RetailerPostpaidTransactionDetails txn : postpaidTxns) {
//            result.add(mapToDto(txn, "Postpaid"));
//        }




        return CompletableFuture.completedFuture(result);
    }

    private SearchTransactionReportResponseDto mapToDto(RetailerPrepaidTransactionDetails txn, String walletType) {
        SearchTransactionReportResponseDto dto = new SearchTransactionReportResponseDto();
        dto.setTransactionId(txn.getTransactionId());
        dto.setHierarchyId(txn.getRetailerId());
        dto.setWalletType(walletType);  //  "Prepaid"
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

    private SearchTransactionReportResponseDto mapToDto(RetailerPostpaidTransactionDetails txn, String walletType) {
        SearchTransactionReportResponseDto dto = new SearchTransactionReportResponseDto();
        dto.setTransactionId(txn.getTransactionId());
        dto.setHierarchyId(txn.getRetailerId());
        dto.setWalletType(walletType);   // "Postpaid"
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





    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchDistributorById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchPartnerById(String txnId, String hierarchyId) {
        List<SearchTransactionReportResponseDto> result = new ArrayList<>();
//
//        List<PartnerPrepaidTransactionDetails> prepaidTxns = partnerPrepaidTransactionDetailsRepository.findByTransactionIdAndHierarchyId(txnId, hierarchyId);
//        for (PartnerPrepaidTransactionDetails txn : prepaidTxns) {
//            result.add(mapToDto(txn, "Prepaid"));
//        }
//
//        List<PartnerPostpaidTransactionDetails> postpaidTxns = partnerPostpaidTransactionDetailsRepository.findByTransactionIdAndHierarchyId(txnId, hierarchyId);
//        for (PartnerPostpaidTransactionDetails txn : postpaidTxns) {
//            result.add(mapToDto(txn, "Postpaid"));
//        }

        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchSuperDistributorById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchWhiteLabelById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchEnterpriseById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    @Async
    public CompletableFuture<List<SearchTransactionReportResponseDto>> searchAdminById(String txnId, String hierarchyId) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    // ...and so on for other hierarchy-specific methods
}
