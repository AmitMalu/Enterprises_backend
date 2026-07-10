package com.im.admin.service;
//
//import com.im.admin.entities.*;
//import com.im.admin.entities.product.Products;
//import com.im.admin.repositories.*;
//import com.im.admin.response.HierarchyProfileResponse;
//import com.im.admin.response.HierarchyProfileResponse.SchemeInfo;
//import com.im.admin.response.HierarchyProfileResponse.WalletInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.*;
//
///**
// * Unified hierarchy profile lookup.
// * Accepts either a hierarchyId (e.g. NFT-RT-001) or a mobile number.
// * Returns: basic info + wallet (prepaid/postpaid) + scheme allocations.
// *
// * @author SHUBHAM KHOPADE
// */
@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyProfileService {
//
//    // ── Hierarchy repos ─────────────────────────────────────────────────────
//    private final RetailerRepository retailerRepo;
//    private final DistributorRepository distributorRepo;
//    private final PartnerRepository partnerRepo;
//    private final SuperDistributorRepository superDistributorRepo;
//    private final WhiteLabelRepository whiteLabelRepo;
//    private final EnterprisesRepository enterprisesRepo;
//    private final AdminRepository adminRepo;
//    private final AdminUserRepository adminUserRepo;
//    private final WhiteLabelUserRepository whiteLabelUserRepo;
//
//    // ── Wallet repos ─────────────────────────────────────────────────────────
//    private final RetailerPrepaidAmountRepository rtPrepaidRepo;
//    private final RetailerPostpaidAmountRepository rtPostpaidRepo;
//    private final DistributorPrepaidAmountRepository dtPrepaidRepo;
//    private final DistributorPostpaidAmountRepository dtPostpaidRepo;
//    private final PartnerPrepaidAmountRepository ptPrepaidRepo;
//    private final PartnerPostpaidAmountRepository ptPostpaidRepo;
//    private final SuperDistributorPrepaidAmountRepository sdPrepaidRepo;
//    private final SuperDistributorPostpaidAmountRepository sdPostpaidRepo;
//    private final WhiteLabelPrepaidAmountRepository wlPrepaidRepo;
//    private final WhiteLabelPostpaidAmountRepository wlPostpaidRepo;
//    private final EnterprisesPrepaidAmountRepository etPrepaidRepo;
//    private final EnterprisesPostpaidAmountRepository etPostpaidRepo;
//    private final AdminPrepaidAmountRepository adPrepaidRepo;
//    private final AdminPostpaidAmountRepository adPostpaidRepo;
//
//    // ── Other repos ──────────────────────────────────────────────────────────
//    private final UserDetailsRepository userDetailsRepo;
//    private final SchemeAllocationRepository schemeAllocationRepo;
//    private final SchemeRepository schemeRepo;
//    private final SubProductsRepository subProductsRepo;
//
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // PUBLIC ENTRY POINT
//    // ─────────────────────────────────────────────────────────────────────────
//
//    /**
//     * Lookup by hierarchyId OR mobileNumber.
//     * Determines the type from the ID format or from UserDetails.
//     */
//    public HierarchyProfileResponse lookup(String search) {
//        if (search == null || search.isBlank()) {
//            throw new IllegalArgumentException("search must not be blank");
//        }
//        search = search.trim();
//
//        // Step 1: resolve hierarchyId + type via UserDetails
//        UserDetails userDetails = userDetailsRepo.findByHierarchyIdOrMobileNumber(search);
//
//        if (userDetails == null) {
//            throw new RuntimeException("No hierarchy found for: " + search);
//        }
//
//        String hierarchyId   = userDetails.getHierarchyId();
//        String hierarchyCode = userDetails.getHierarchyCode() != null
//                ? userDetails.getHierarchyCode().toUpperCase().trim()
//                : extractCodeFromId(hierarchyId);
//
//        log.info("Profile lookup — id={} code={}", hierarchyId, hierarchyCode);
//
//        // Step 2: build response based on type
//        return switch (hierarchyCode) {
//            case "RT"  -> buildForRetailer(hierarchyId, userDetails);
//            case "DT"  -> buildForDistributor(hierarchyId, userDetails);
//            case "PT"  -> buildForPartner(hierarchyId, userDetails);
//            case "SD"  -> buildForSuperDistributor(hierarchyId, userDetails);
//            case "WL"  -> buildForWhiteLabel(hierarchyId, userDetails);
//            case "ET"  -> buildForEnterprise(hierarchyId, userDetails);
//            case "AD"  -> buildForAdmin(hierarchyId, userDetails);
//            case "AU"  -> buildForAdminUser(hierarchyId, userDetails);
//            case "WAU" -> buildForWLUser(hierarchyId, userDetails);
//            default    -> throw new RuntimeException("Unsupported hierarchy type: " + hierarchyCode);
//        };
//    }
//
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // HIERARCHY-SPECIFIC BUILDERS
//    // ─────────────────────────────────────────────────────────────────────────
//
//    private HierarchyProfileResponse buildForRetailer(String id, UserDetails ud) {
//        Retailer e = retailerRepo.findByRetailerId(id)
//                .orElseThrow(() -> new RuntimeException("Retailer not found: " + id));
//
//        WalletInfo prepaid  = rtPrepaidRepo.findByRetailerId_RetailerId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = rtPostpaidRepo.findByRetailerId_RetailerId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "RT", "Retailer", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), e.getDob() != null ? e.getDob().atStartOfDay() : null,
//                e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForDistributor(String id, UserDetails ud) {
//        Distributor e = distributorRepo.findByDistributorId(id)
//                .orElseThrow(() -> new RuntimeException("Distributor not found: " + id));
//
//        WalletInfo prepaid  = dtPrepaidRepo.findByDistributorId_DistributorId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = dtPostpaidRepo.findByDistributorId_DistributorId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "DT", "Distributor", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), null, e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForPartner(String id, UserDetails ud) {
//        Partner e = partnerRepo.findByPartnerId(id)
//                .orElseThrow(() -> new RuntimeException("Partner not found: " + id));
//
//        WalletInfo prepaid  = ptPrepaidRepo.findByPartnerId_PartnerId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = ptPostpaidRepo.findByPartnerId_PartnerId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "PT", "Partner", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), null, e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForSuperDistributor(String id, UserDetails ud) {
//        SuperDistributor e = superDistributorRepo.findBySuperDistributorId(id)
//                .orElseThrow(() -> new RuntimeException("SuperDistributor not found: " + id));
//
//        WalletInfo prepaid  = sdPrepaidRepo.findBySuperDistributorId_SuperDistributorId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = sdPostpaidRepo.findBySuperDistributorId_SuperDistributorId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "SD", "Super Distributor", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), null, e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//
//    private HierarchyProfileResponse buildForWhiteLabel(String id, UserDetails ud) {
//        WhiteLabels e = whiteLabelRepo.findByWhiteLabelId(id)
//                .orElseThrow(() -> new RuntimeException("WhiteLabel not found: " + id));
//
//        WalletInfo prepaid  = wlPrepaidRepo.findByWhiteLabelId_WhiteLabelId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = wlPostpaidRepo.findByWhiteLabelId_WhiteLabelId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "WL", "White Label", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), null, e.getCreatedOn(), e.getCreatedBy(), e.getEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForEnterprise(String id, UserDetails ud) {
//        Enterprises e = enterprisesRepo.findByEnterpriseId(id)
//                .orElseThrow(() -> new RuntimeException("Enterprise not found: " + id));
//
//        WalletInfo prepaid  = etPrepaidRepo.findByEnterprise_EnterpriseId(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = etPostpaidRepo.findByEnterprise_EnterpriseId(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "ET", "Enterprise", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), e.getTitle(),
//                e.getStatus(), null, e.getCreatedOn(), e.getCreatedBy(), e.getEditedOn(), e.getEditedBy(),
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForAdmin(String id, UserDetails ud) {
//        Admin e = adminRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Admin not found: " + id));
//
//        WalletInfo prepaid  = adPrepaidRepo.findByAdminId_Id(id)
//                .map(w -> walletInfo("PREPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//        WalletInfo postpaid = adPostpaidRepo.findByAdminId_Id(id)
//                .map(w -> walletInfo("POSTPAID", w.getTotalCash(), w.getAvailableBalance(),
//                        w.getUsedCash(), w.getCutOfAmount(), w.getLastUpdatedAmount(), w.getLastUpdatedDate()))
//                .orElse(null);
//
//        return base(id, "AD", "Admin", null,
//                e.getName(), e.getMobileNumber(), e.getEmailId(), null,
//                e.getStatus(), null, e.getCreatedOn(), null, e.getEditedOn(), null,
//                ud, prepaid, postpaid, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForAdminUser(String id, UserDetails ud) {
//        AdminUser e = adminUserRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("AdminUser not found: " + id));
//
//        return base(id, "AU", "Admin User", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), null,
//                Boolean.TRUE.equals(e.getStatus()) ? "ACTIVE" : "INACTIVE",
//                null, e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, null, null, schemes(id));
//    }
//
//    private HierarchyProfileResponse buildForWLUser(String id, UserDetails ud) {
//        WhiteLabelUser e = whiteLabelUserRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("WhiteLabelUser not found: " + id));
//
//        return base(id, "WAU", "White Label User", e.getParentAccount(),
//                e.getName(), e.getMobileNumber(), e.getEmailId(), null,
//                Boolean.TRUE.equals(e.getStatus()) ? "ACTIVE" : "INACTIVE",
//                null, e.getCreatedOn(), e.getCreatedBy(), e.getLastEditedOn(), e.getEditedBy(),
//                ud, null, null, schemes(id));
//    }
//
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // SCHEME ALLOCATIONS
//    // ─────────────────────────────────────────────────────────────────────────
//
//    private List<SchemeInfo> schemes(String hierarchyId) {
//        List<SchemeAllocation> allocations = schemeAllocationRepo.findAllByHierarchyId(hierarchyId);
//        if (allocations == null || allocations.isEmpty()) return Collections.emptyList();
//
//        List<SchemeInfo> result = new ArrayList<>();
//        for (SchemeAllocation alloc : allocations) {
//
//            // Resolve subProduct name
//            String subProductName = subProductsRepo
//                    .findBySubProductCode(alloc.getSubProductCode())
//                    .map(SubProducts::getSubProductTitle)
//                    .orElse(alloc.getSubProductCode());
//
//            // Resolve product name via subProduct → product relationship
//            String productName = subProductsRepo
//                    .findBySubProductCode(alloc.getSubProductCode())
//                    .map(sp -> sp.getProduct() != null ? sp.getProduct().getName() : alloc.getProductCode())
//                    .orElse(alloc.getProductCode());
//
//            // Resolve scheme name + active flag
//            String schemeName  = null;
//            Boolean schemeActive = null;
//            Optional<Scheme> scheme = schemeRepo.findBySchemeCode(alloc.getSchemeCode());
//            if (scheme.isPresent()) {
//                schemeName   = scheme.get().getSchemeName();
//                schemeActive = scheme.get().getIsActive();
//            }
//
//            result.add(SchemeInfo.builder()
//                    .productCode(alloc.getProductCode())
//                    .productName(productName)
//                    .subProductCode(alloc.getSubProductCode())
//                    .subProductName(subProductName)
//                    .schemeCode(alloc.getSchemeCode())
//                    .schemeName(schemeName)
//                    .schemeActive(schemeActive)
//                    .build());
//        }
//        return result;
    }
//
