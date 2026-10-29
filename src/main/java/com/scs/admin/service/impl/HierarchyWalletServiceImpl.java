package com.scs.admin.service.impl;

import com.scs.admin.repositories.*;
import com.scs.admin.request.UpdateLienAmountRequest;
import com.scs.admin.response.HierarchyWalletReportDto;
import com.scs.admin.service.HierarchyWalletService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HierarchyWalletServiceImpl implements HierarchyWalletService {

    private final RetailerPrepaidAmountRepository          retailerPrepaidRepo;
    private final RetailerPostpaidAmountRepository         retailerPostpaidRepo;

    private final DistributorPrepaidAmountRepository       distributorPrepaidRepo;
    private final DistributorPostpaidAmountRepository      distributorPostpaidRepo;

    private final PartnerPrepaidAmountRepository           partnerPrepaidRepo;
    private final PartnerPostpaidAmountRepository          partnerPostpaidRepo;

    private final SuperDistributorPrepaidAmountRepository  sdPrepaidRepo;
    private final SuperDistributorPostpaidAmountRepository sdPostpaidRepo;

    private final WhiteLabelPrepaidAmountRepository        wlPrepaidRepo;
    private final WhiteLabelPostpaidAmountRepository       wlPostpaidRepo;

    private final EnterprisesPrepaidAmountRepository       etPrepaidRepo;
    private final EnterprisesPostpaidAmountRepository      etPostpaidRepo;

    private final AdminPrepaidAmountRepository             adminPrepaidRepo;
    private final AdminPostpaidAmountRepository            adminPostpaidRepo;

    private final CommonMethods                            commonMethods;

    // -------------------------------------------------------------------------
    // GET wallet details
    // -------------------------------------------------------------------------

    @Override
    public HierarchyWalletReportDto getWalletDetails(String hierarchyId) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        log.info("getWalletDetails | id={} type={}", hierarchyId, hierarchyType);

        HierarchyWalletReportDto dto = new HierarchyWalletReportDto();
        dto.setHierarchyType(hierarchyType);
        dto.setHierarchyId(hierarchyId);

        switch (hierarchyType.toUpperCase()) {

            case "RT" -> {
                var prepaid = retailerPrepaidRepo
                        .findByRetailerId_RetailerId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Retailer prepaid wallet not found: " + hierarchyId));
                var postpaid = retailerPostpaidRepo
                        .findByRetailerId_RetailerId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Retailer postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getRetailerId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "DT" -> {
                var prepaid = distributorPrepaidRepo
                        .findByDistributorId_DistributorId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Distributor prepaid wallet not found: " + hierarchyId));
                var postpaid = distributorPostpaidRepo
                        .findByDistributorId_DistributorId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Distributor postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getDistributorId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "PT" -> {
                var prepaid = partnerPrepaidRepo
                        .findByPartnerId_PartnerId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Partner prepaid wallet not found: " + hierarchyId));
                var postpaid = partnerPostpaidRepo
                        .findByPartnerId_PartnerId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Partner postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getPartnerId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "SD" -> {
                var prepaid = sdPrepaidRepo
                        .findBySuperDistributorId_SuperDistributorId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("SuperDistributor prepaid wallet not found: " + hierarchyId));
                var postpaid = sdPostpaidRepo
                        .findBySuperDistributorId_SuperDistributorId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("SuperDistributor postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getSuperDistributorId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "WL" -> {
                var prepaid = wlPrepaidRepo
                        .findByWhiteLabelId_WhiteLabelId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("WhiteLabel prepaid wallet not found: " + hierarchyId));
                var postpaid = wlPostpaidRepo
                        .findByWhiteLabelId_WhiteLabelId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("WhiteLabel postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getWhiteLabelId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "ET" -> {
                var prepaid = etPrepaidRepo
                        .findByEnterprise_EnterpriseId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Enterprise prepaid wallet not found: " + hierarchyId));
                var postpaid = etPostpaidRepo
                        .findByEnterprise_EnterpriseId(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Enterprise postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getEnterprise().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            case "AD" -> {
                var prepaid = adminPrepaidRepo
                        .findByAdminId_Id(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Admin prepaid wallet not found: " + hierarchyId));
                var postpaid = adminPostpaidRepo
                        .findByAdminId_Id(hierarchyId)
                        .orElseThrow(() -> new IllegalArgumentException("Admin postpaid wallet not found: " + hierarchyId));

                dto.setFullName(prepaid.getAdminId().getName());
                dto.setPrepaidBalance(prepaid.getAvailableBalance());
                dto.setPrepaidCutoffAmount(prepaid.getCutOfAmount());
                dto.setRemark(prepaid.getRemark());
                dto.setPostpaidBalance(postpaid.getAvailableBalance());
                dto.setPostpaidCutoffAmount(postpaid.getCutOfAmount());
                dto.setLastUpdatedDate(prepaid.getLastUpdatedDate());
            }

            default -> throw new IllegalArgumentException("Unknown hierarchyType: " + hierarchyType);
        }

        return dto;
    }

    // UPDATE lien amount
    @Override
    @Transactional
    public HierarchyWalletReportDto updateLienAmount(UpdateLienAmountRequest request) {

        log.info("updateLienAmount | id={}", request.getHierarchyId());

        String id       = request.getHierarchyId();
        String type     = commonMethods.extractHierarchyType(id);
        LocalDate today = LocalDate.now();

        switch (type) {

            case "RT" -> {
                var prepaid = retailerPrepaidRepo
                        .findByRetailerId_RetailerId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Retailer prepaid wallet not found: " + id));
                var postpaid = retailerPostpaidRepo
                        .findByRetailerId_RetailerId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Retailer postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                retailerPrepaidRepo.save(prepaid);
                retailerPostpaidRepo.save(postpaid);
            }

            case "DT" -> {
                var prepaid = distributorPrepaidRepo
                        .findByDistributorId_DistributorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Distributor prepaid wallet not found: " + id));
                var postpaid = distributorPostpaidRepo
                        .findByDistributorId_DistributorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Distributor postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                distributorPrepaidRepo.save(prepaid);
                distributorPostpaidRepo.save(postpaid);
            }

            case "PT" -> {
                var prepaid = partnerPrepaidRepo
                        .findByPartnerId_PartnerId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Partner prepaid wallet not found: " + id));
                var postpaid = partnerPostpaidRepo
                        .findByPartnerId_PartnerId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Partner postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                partnerPrepaidRepo.save(prepaid);
                partnerPostpaidRepo.save(postpaid);
            }

            case "SD" -> {
                var prepaid = sdPrepaidRepo
                        .findBySuperDistributorId_SuperDistributorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("SuperDistributor prepaid wallet not found: " + id));
                var postpaid = sdPostpaidRepo
                        .findBySuperDistributorId_SuperDistributorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("SuperDistributor postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                sdPrepaidRepo.save(prepaid);
                sdPostpaidRepo.save(postpaid);
            }

            case "WL" -> {
                var prepaid = wlPrepaidRepo
                        .findByWhiteLabelId_WhiteLabelId(id)
                        .orElseThrow(() -> new IllegalArgumentException("WhiteLabel prepaid wallet not found: " + id));
                var postpaid = wlPostpaidRepo
                        .findByWhiteLabelId_WhiteLabelId(id)
                        .orElseThrow(() -> new IllegalArgumentException("WhiteLabel postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                wlPrepaidRepo.save(prepaid);
                wlPostpaidRepo.save(postpaid);
            }

            case "ET" -> {
                var prepaid = etPrepaidRepo
                        .findByEnterprise_EnterpriseId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Enterprise prepaid wallet not found: " + id));
                var postpaid = etPostpaidRepo
                        .findByEnterprise_EnterpriseId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Enterprise postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                etPrepaidRepo.save(prepaid);
                etPostpaidRepo.save(postpaid);
            }

            case "AD" -> {
                var prepaid = adminPrepaidRepo
                        .findByAdminId_Id(id)
                        .orElseThrow(() -> new IllegalArgumentException("Admin prepaid wallet not found: " + id));
                var postpaid = adminPostpaidRepo
                        .findByAdminId_Id(id)
                        .orElseThrow(() -> new IllegalArgumentException("Admin postpaid wallet not found: " + id));

//                validateLien("Prepaid",  request.getPrepaidCutoffAmount(),  prepaid.getAvailableBalance());
//                validateLien("Postpaid", request.getPostpaidCutoffAmount(), postpaid.getAvailableBalance());

                prepaid.setCutOfAmount(request.getPrepaidCutoffAmount());
                prepaid.setRemark(request.getRemark());
                prepaid.setLastUpdatedDate(today);

                postpaid.setCutOfAmount(request.getPostpaidCutoffAmount());
                postpaid.setLastUpdatedDate(today);

                adminPrepaidRepo.save(prepaid);
                adminPostpaidRepo.save(postpaid);
            }

            default -> throw new IllegalArgumentException("Unknown hierarchyType: " + type);
        }

        // Return fresh wallet state after update
        return getWalletDetails(id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateLien(String walletLabel, BigDecimal lien, BigDecimal balance) {
        if (lien == null) return;
        if (balance == null) balance = BigDecimal.ZERO;
        if (lien.compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    walletLabel + " lien amount (" + lien + ") cannot exceed available balance (" + balance + ")"
            );
        }
    }
}
