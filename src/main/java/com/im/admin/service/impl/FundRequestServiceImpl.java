package com.im.admin.service.impl;


import com.im.admin.dto.ChildUserDto;
import com.im.admin.dto.FundRequestDto;
import com.im.admin.entities.FundRequest;
import com.im.admin.repositories.*;
import com.im.admin.service.FundRequestService;
import com.im.admin.util.CommonMethods;
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
    private  PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private  RetailerRepository retailerRepository;



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

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
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

//    @Override
//    public List<?> getChildUsers(String parentHierarchyId) {
//        String parentType = commonMethods.extractHierarchyType(parentHierarchyId); // e.g., DT
//        String childEntityName = hierarchyChildren.get(parentType); // e.g., retailer
//
//        if (childEntityName == null)
//            throw new RuntimeException("No child entity found for hierarchy type: " + parentType);
//
//        return switch (childEntityName) {
////            case "admin" -> adminRepository.findByParentAccountContaining(parentType);
//            case "enterprises" -> enterprisesRepository.findByParentAccountContaining(parentType);
//            case "white_labels" -> whiteLabelRepository.findByParentAccountContaining(parentType);
//            case "super_distributor" -> superDistributorRepository.findByParentAccountContaining(parentType);
//            case "partner" -> partnerRepository.findByParentAccountContaining(parentType);
//            case "distributor" -> distributorRepository.findByParentAccountContaining(parentType);
//            case "retailer" -> retailerRepository.findByParentAccountContaining(parentType);
//            default -> throw new RuntimeException("Invalid entity name: " + childEntityName);
//        };
//    }
//
    private static final Map<String, String> hierarchyChildren = Map.of(
            "SA", "admin",
            "AD", "enterprises",
            "ET", "white_labels",
            "WL", "super_distributor",
            "SD", "partner",
            "PT", "distributor",
            "DT", "retailer"
    );

    @Override
    public List<ChildUserDto> getChildUsers(String parentHierarchyId) {
        String parentType = commonMethods.extractHierarchyType(parentHierarchyId); // e.g., DT
        String childEntity = hierarchyChildren.get(parentType); // e.g., retailer

        if (childEntity == null)
            throw new RuntimeException("No child entity found for hierarchy type: " + parentType);

        return switch (childEntity) {
            case "enterprises" -> enterprisesRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                    .map(e -> new ChildUserDto(
                            e.getEnterpriseId(), e.getParentAccount(), e.getAbbreviation(), e.getTitle(),
                            e.getName(), e.getMobileNumber(), e.getEmailId(), e.getDob(), e.getStatus()
                    )).collect(Collectors.toList());

            case "white_labels" -> whiteLabelRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                    .map(w -> new ChildUserDto(
                            w.getWhiteLabelId(), w.getParentAccount(), w.getAbbreviation(), w.getTitle(),
                            w.getName(), w.getMobileNumber(), w.getEmailId(), w.getDob(), w.getStatus()
                    )).collect(Collectors.toList());

            case "super_distributor" -> superDistributorRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                    .map(s -> new ChildUserDto(
                            s.getSuperDistributorId(), s.getParentAccount(), s.getAbbreviation(), s.getTitle(),
                            s.getName(), s.getMobileNumber(), s.getEmailId(), s.getDob(), s.getStatus()
                    )).collect(Collectors.toList());

            case "partner" -> partnerRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                    .map(p -> new ChildUserDto(
                            p.getPartnerId(), p.getParentAccount(), p.getAbbreviation(), p.getTitle(),
                            p.getName(), p.getMobileNumber(), p.getEmailId(), p.getDob(), p.getStatus()
                    )).collect(Collectors.toList());

            case "distributor" -> distributorRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                    .map(d -> new ChildUserDto(
                            d.getDistributorId(), d.getParentAccount(), d.getAbbreviation(), d.getTitle(),
                            d.getName(), d.getMobileNumber(), d.getEmailId(), d.getDob(), d.getStatus()
                    )).collect(Collectors.toList());

            case "retailer" -> retailerRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    .map(r -> new ChildUserDto(
                            r.getRetailerId(), r.getParentAccount(), r.getAbbreviation(), r.getTitle(),
                            r.getName(), r.getMobileNumber(), r.getEmailId(), r.getDob(), r.getStatus()
                    )).collect(Collectors.toList());

            default -> throw new RuntimeException("Invalid entity name: " + childEntity);
        };
    }


}
