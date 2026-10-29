package com.scs.admin.service.impl;


import com.scs.admin.dto.ChildUserDto;
import com.scs.admin.dto.FundRequestDto;
import com.scs.admin.entities.FundRequest;
import com.scs.admin.repositories.*;
import com.scs.admin.service.FundRequestService;
import com.scs.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FundRequestServiceImpl implements FundRequestService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FundRequestRepository fundRequestRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RetailerRepository retailerRepository;


    @Transactional
    @Override
    public void raiseFundRequest(FundRequestDto fundRequestDto) {
        FundRequest fundRequest = new FundRequest();
        fundRequest.setRequesterHierarchyId(fundRequestDto.getRequesterHierarchyId());
        fundRequest.setRequestedAmount(fundRequestDto.getRequestedAmount());
        fundRequest.setWalletType(fundRequestDto.getWalletType());

        String currentId = fundRequestDto.getRequesterHierarchyId();

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                throw new RuntimeException("No approver found for hierarchy: " + fundRequestDto.getRequesterHierarchyId());
            }

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                whiteLabelRepository.findById(parentId).ifPresent(wl -> {
                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        fundRequest.setApproveOrRejectHierarchyId(wl.getWhiteLabelId());
                        fundRequest.setApproveOrRejectDate(LocalDate.now());
                        fundRequest.setApproveOrRejectTime(LocalTime.now());
                    }
                });
                if (fundRequest.getApproveOrRejectHierarchyId() != null) {
                    break;
                }
            }

            if ("SCS".equals(parentType) || "AD".equals(parentType)) {
                adminRepository.findById(parentId).ifPresent(ad -> {
                    fundRequest.setApproveOrRejectHierarchyId(ad.getId());
                    fundRequest.setApproveOrRejectDate(LocalDate.now());
                    fundRequest.setApproveOrRejectTime(LocalTime.now());
                });
                if (fundRequest.getApproveOrRejectHierarchyId() != null) {
                    break;
                }
            }

            currentId = parentId;
        }

        fundRequestRepository.save(fundRequest);
    }

//    @Transactional
//    @Override
//    public void takeActionOnFundRequest(FundRequestActionDto actionDto) {
//        FundRequest fundRequest = fundRequestRepository.findById(actionDto.getFundRequestId())
//                .orElseThrow(() -> new RuntimeException("Fund Request not found"));
//
//        if (fundRequest.getStatus() != ConstantStatus.PENDING) {
//            throw new RuntimeException("Fund Request already processed");
//        }
//
//        ConstantStatus newStatus = actionDto.getStatus();
//        fundRequest.setStatus(newStatus);
//        fundRequest.setRemarks(actionDto.getRemarks());
//        fundRequest.setApproveOrRejectDate(LocalDate.now());
//        fundRequest.setApproveOrRejectTime(LocalTime.now());
//
//        if (newStatus == ConstantStatus.APPROVED) {
//            // Fetch details
//            String requesterId = fundRequest.getRequesterHierarchyId();
//            String approverId = fundRequest.getApproveOrRejectHierarchyId();
//            WalletType walletType = fundRequest.getWalletType();
//            BigDecimal amount = fundRequest.getRequestedAmount();
//
//            String baseTransactionId = commonMethods.generateBaseTransactionId();
//
//            // Step 1: Debit approver's wallet
//            AbstractWalletAmountTable approverWallet = commonMethods.getWalletByType(walletType.name(), approverId);
//            BigDecimal approverBalance = approverWallet.getAvailableBalance(); // assuming your wallet entity has "amount" field
//
//            // Check if sufficient balance
//            if (approverBalance.compareTo(amount) < 0) {
//                throw new RuntimeException("Insufficient balance in approver's wallet: " + approverId);
//            }
//
//            updateWallet(approverWallet, approverId, walletType, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT");
//
//            // Step 2: Credit requester's wallet
//            AbstractWalletAmountTable requesterWallet = commonMethods.getWalletByType(walletType.name(), requesterId);
//            updateWallet(requesterWallet, requesterId, walletType, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT");
//        }
//
//        fundRequestRepository.save(fundRequest);
//    }

    @Override
    public List<FundRequest> fetchAllRecords() {
        return fundRequestRepository.findAll();
    }


}
