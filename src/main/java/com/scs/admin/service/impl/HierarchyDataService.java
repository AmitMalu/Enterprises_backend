package com.scs.admin.service.impl;

/**
 * @author SHUBHAM KHOPADE
 */

import com.scs.admin.entities.*;
import com.scs.admin.entities.product.Products;
import com.scs.admin.exception.ResourceNotFoundException;
import com.scs.admin.repositories.*;
import com.scs.admin.response.HierarchyOptionResponse;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import com.scs.admin.util.UnderHierarchyProjection;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HierarchyDataService {

    private final UserDetailsRepository userDetailsRepository;

    private final RetailerRepository retailerRepository;
    private final DistributorRepository distributorRepository;
    private final PartnerRepository partnerRepository;
    private final SuperDistributorRepository
            superDistributorRepository;
    private final WhiteLabelRepository whiteLabelsRepository;
    private final AdminRepository adminRepository;
    private final AdminUserRepository adminUserRepository;
    private final WhiteLabelUserRepository
            whiteLabelUserRepository;

    private final SchemeAllocationRepository
            schemeAllocationRepository;
    private final ProductsRepository productsRepository;
    private final SubProductsRepository subProductsRepository;
    private final SchemeRepository schemeRepository;

    private final HierarchyWalletService
            hierarchyWalletService;

    private final AccessHierarchyPathRepository
            accessHierarchyPathRepository;

    private final AccessHierarchyNodeRepository
            accessHierarchyNodeRepository;

    private final TokenUtil tokenUtil;

    private static final Set<String> ALLOWED_HIERARCHY_CODES =
            Set.of(
                    "RT",
                    "DT",
                    "PT",
                    "SD"
            );

    private final CommonMethods commonMethods;

    public Map<String, Object> getCompleteHierarchyData(
            String hierarchyId,
            String mobileNumber
    ) {

        String normalizedHierarchyId =
                trimToNull(hierarchyId);

        String normalizedMobileNumber =
                trimToNull(mobileNumber);

        validateRequest(
                normalizedHierarchyId,
                normalizedMobileNumber
        );

        UserDetails userDetails =
                findUserDetails(
                        normalizedHierarchyId,
                        normalizedMobileNumber
                );

        String resolvedHierarchyId =
                userDetails.getHierarchyId();

        String hierarchyCode =
                normalizeHierarchyCode(
                        userDetails.getHierarchyCode()
                );

        log.info(
                "Fetching complete hierarchy profile. hierarchyId: {}, hierarchyCode: {}",
                resolvedHierarchyId,
                hierarchyCode
        );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "searchInformation",
                buildSearchInformation(
                        normalizedHierarchyId,
                        normalizedMobileNumber,
                        resolvedHierarchyId,
                        hierarchyCode
                )
        );

        response.put(
                "userInformation",
                buildUserInformation(userDetails)
        );

        response.put(
                "basicInformation",
                getBasicInformation(
                        hierarchyCode,
                        resolvedHierarchyId
                )
        );

        response.put(
                "walletInformation",
                hierarchyWalletService
                        .getWalletInformation(
                                hierarchyCode,
                                resolvedHierarchyId
                        )
        );

        response.put(
                "assignments",
                getAssignmentInformation(
                        resolvedHierarchyId
                )
        );

        /*
         * New section:
         * Returns every hierarchy available below
         * the searched hierarchy.
         */
        response.put(
                "underHierarchy",
                getUnderHierarchyInformation(
                        resolvedHierarchyId
                )
        );

        return response;
    }

    private void validateRequest(
            String hierarchyId,
            String mobileNumber
    ) {

        if (hierarchyId == null &&
                mobileNumber == null) {

            throw new IllegalArgumentException(
                    "Either hierarchyId or mobileNumber is required"
            );
        }

        if (hierarchyId != null &&
                mobileNumber != null) {

            throw new IllegalArgumentException(
                    "Provide either hierarchyId or mobileNumber, not both"
            );
        }

        if (mobileNumber != null &&
                !mobileNumber.matches("\\d{10}")) {

            throw new IllegalArgumentException(
                    "Mobile number must contain exactly 10 digits"
            );
        }
    }

    private UserDetails findUserDetails(
            String hierarchyId,
            String mobileNumber
    ) {

        List<UserDetails> users;

        if (hierarchyId != null) {

            users = userDetailsRepository
                    .findAllByHierarchyIdIgnoreCase(
                            hierarchyId
                    );

        } else {

            users = userDetailsRepository
                    .findAllByMobileNumber(
                            mobileNumber
                    );
        }

        if (users.isEmpty()) {
            throw new EntityNotFoundException(
                    "No hierarchy record found for the supplied search value"
            );
        }

        if (users.size() > 1) {

            log.error(
                    "Multiple user_details records found. hierarchyId: {}, mobile supplied: {}, count: {}",
                    hierarchyId,
                    mobileNumber != null,
                    users.size()
            );

            throw new IllegalStateException(
                    "Multiple hierarchy records found for the supplied search value"
            );
        }

        return users.get(0);
    }

    private String normalizeHierarchyCodeOption(
            String hierarchyCode
    ) {

        if (hierarchyCode == null ||
                hierarchyCode.isBlank()) {

            throw new IllegalStateException(
                    "Hierarchy code is missing in user_details"
            );
        }

        String code =
                hierarchyCode
                        .trim()
                        .toUpperCase();

        return switch (code) {

            case "RETAILER" -> "RT";

            case "DISTRIBUTOR" -> "DT";

            case "PARTNER" -> "PT";

            case "SUPER DISTRIBUTOR",
                 "SUPER_DISTRIBUTOR" -> "SD";

            case "WHITE LABEL",
                 "WHITE_LABEL" -> "WL";

            case "ADMIN" -> "AD";

            case "ADMIN USER",
                 "ADMIN_USER" -> "AU";

            case "WHITE LABEL USER",
                 "WHITE_LABEL_USER",
                 "WHITE LABEL ADMIN USER",
                 "WHITE_LABEL_ADMIN_USER" -> "WLAU";

            default -> code;
        };
    }

    private Map<String, Object> buildSearchInformation(
            String searchedHierarchyId,
            String searchedMobileNumber,
            String resolvedHierarchyId,
            String hierarchyCode
    ) {

        Map<String, Object> information =
                new LinkedHashMap<>();

        if (searchedHierarchyId != null) {

            information.put(
                    "searchType",
                    "HIERARCHY_ID"
            );

            information.put(
                    "searchValue",
                    searchedHierarchyId
            );

        } else {

            information.put(
                    "searchType",
                    "MOBILE_NUMBER"
            );

            information.put(
                    "searchValue",
                    searchedMobileNumber
            );
        }

        information.put(
                "resolvedHierarchyId",
                resolvedHierarchyId
        );

        information.put(
                "hierarchyCode",
                hierarchyCode
        );

        return information;
    }

    private Map<String, Object> buildUserInformation(
            UserDetails user
    ) {

        Map<String, Object> information =
                new LinkedHashMap<>();

        information.put(
                "hierarchyId",
                user.getHierarchyId()
        );

        information.put(
                "hierarchyCode",
                user.getHierarchyCode()
        );

        information.put(
                "mobileNumber",
                user.getMobileNumber()
        );

        information.put(
                "emailId",
                user.getEmailId()
        );

        information.put(
                "loginUserName",
                user.getLoginUserName()
        );

        information.put(
                "userLoginStatus",
                user.getUserLoginStatus()
        );

        information.put(
                "userStatus",
                user.getUserStatus()
        );

        information.put(
                "lastLoginDate",
                user.getLastLoginDate()
        );

        information.put(
                "lastLoginTime",
                user.getLastLoginTime()
        );

        information.put(
                "channel",
                user.getChannel()
        );

        information.put(
                "geoLocation",
                user.getGeoLocation()
        );

        information.put(
                "isBlocked",
                user.getIsBlocked()
        );

        information.put(
                "failedAttempts",
                user.getFailedAttempts()
        );

        information.put(
                "permission",
                user.getPermission()
        );

        information.put(
                "parentAccount",
                user.getParentAccount()
        );

        information.put(
                "createdDate",
                user.getCreatedDate()
        );

        information.put(
                "createdTime",
                user.getCreatedTime()
        );

        information.put(
                "createdBy",
                user.getCreatedBy()
        );

        information.put(
                "editedDate",
                user.getEditedDate()
        );

        information.put(
                "editedTime",
                user.getEditedTime()
        );

        information.put(
                "editedBy",
                user.getEditedBy()
        );

        /*
         * Do not return login password, OTP, OTP expiry
         * or lock/security-sensitive values unnecessarily.
         */

        return information;
    }

    private Map<String, Object> getBasicInformation(
            String hierarchyCode,
            String hierarchyId
    ) {

        return switch (hierarchyCode) {

            case "RT" -> retailerRepository
                    .findByRetailerId(hierarchyId)
                    .map(this::mapRetailer)
                    .orElseThrow(() ->
                            notFound(
                                    "Retailer",
                                    hierarchyId
                            )
                    );

            case "DT" -> distributorRepository
                    .findByDistributorId(hierarchyId)
                    .map(this::mapDistributor)
                    .orElseThrow(() ->
                            notFound(
                                    "Distributor",
                                    hierarchyId
                            )
                    );

            case "PT" -> partnerRepository
                    .findByPartnerId(hierarchyId)
                    .map(this::mapPartner)
                    .orElseThrow(() ->
                            notFound(
                                    "Partner",
                                    hierarchyId
                            )
                    );

            case "SD" -> superDistributorRepository
                    .findBySuperDistributorId(
                            hierarchyId
                    )
                    .map(this::mapSuperDistributor)
                    .orElseThrow(() ->
                            notFound(
                                    "Super Distributor",
                                    hierarchyId
                            )
                    );

            case "WL" -> whiteLabelsRepository
                    .findByWhiteLabelId(hierarchyId)
                    .map(this::mapWhiteLabel)
                    .orElseThrow(() ->
                            notFound(
                                    "White Label",
                                    hierarchyId
                            )
                    );

            case "AD" -> adminRepository
                    .findById(hierarchyId)
                    .map(this::mapAdmin)
                    .orElseThrow(() ->
                            notFound(
                                    "Admin",
                                    hierarchyId
                            )
                    );

            case "AU" -> adminUserRepository
                    .findById(hierarchyId)
                    .map(this::mapAdminUser)
                    .orElseThrow(() ->
                            notFound(
                                    "Admin User",
                                    hierarchyId
                            )
                    );

            case "WLAU" -> whiteLabelUserRepository
                    .findById(hierarchyId)
                    .map(this::mapWhiteLabelUser)
                    .orElseThrow(() ->
                            notFound(
                                    "White Label User",
                                    hierarchyId
                            )
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported hierarchy code: " +
                            hierarchyCode
            );
        };
    }

    private Map<String, Object> getAssignmentInformation(
            String hierarchyId
    ) {

        List<SchemeAllocation> allocations =
                schemeAllocationRepository
                        .findAllByHierarchyIdOrderByIdAsc(
                                hierarchyId
                        );

        if (allocations.isEmpty()) {

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put("assigned", false);
            result.put("totalAssignments", 0);
            result.put(
                    "items",
                    Collections.emptyList()
            );

            return result;
        }

        Set<String> productCodes =
                allocations.stream()
                        .map(
                                SchemeAllocation::
                                        getProductCode
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Set<String> subProductCodes =
                allocations.stream()
                        .map(
                                SchemeAllocation::
                                        getSubProductCode
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Set<String> schemeCodes =
                allocations.stream()
                        .map(
                                SchemeAllocation::
                                        getSchemeCode
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<String, Products> productMap =
                productsRepository
                        .findAllByProductCodeIn(
                                productCodes
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Products::
                                                getProductCode,
                                        Function.identity(),
                                        (
                                                first,
                                                duplicate
                                        ) -> first
                                )
                        );

        Map<String, SubProducts> subProductMap =
                subProductsRepository
                        .findAllBySubProductCodeIn(
                                subProductCodes
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        SubProducts::
                                                getSubProductCode,
                                        Function.identity(),
                                        (
                                                first,
                                                duplicate
                                        ) -> first
                                )
                        );

        Map<String, Scheme> schemeMap =
                schemeRepository
                        .findAllBySchemeCodeIn(
                                schemeCodes
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Scheme::getSchemeCode,
                                        Function.identity(),
                                        (
                                                first,
                                                duplicate
                                        ) -> first
                                )
                        );

        List<Map<String, Object>> items =
                new ArrayList<>();

        for (SchemeAllocation allocation :
                allocations) {

            Map<String, Object> item =
                    new LinkedHashMap<>();

            item.put(
                    "allocationId",
                    allocation.getId()
            );

            item.put(
                    "hierarchyId",
                    allocation.getHierarchyId()
            );

            item.put(
                    "product",
                    mapProduct(
                            allocation.getProductCode(),
                            productMap.get(
                                    allocation
                                            .getProductCode()
                            )
                    )
            );

            item.put(
                    "subProduct",
                    mapSubProduct(
                            allocation
                                    .getSubProductCode(),
                            subProductMap.get(
                                    allocation
                                            .getSubProductCode()
                            )
                    )
            );

            item.put(
                    "scheme",
                    mapScheme(
                            allocation.getSchemeCode(),
                            schemeMap.get(
                                    allocation.getSchemeCode()
                            )
                    )
            );

            items.add(item);
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("assigned", true);

        result.put(
                "totalAssignments",
                items.size()
        );

        result.put("items", items);

        return result;
    }

    private Map<String, Object> mapProduct(
            String allocationProductCode,
            Products product
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "allocatedProductCode",
                allocationProductCode
        );

        if (product == null) {

            data.put(
                    "masterRecordFound",
                    false
            );

            return data;
        }

        data.put("masterRecordFound", true);
        data.put("id", product.getId());
        data.put("name", product.getName());

        data.put(
                "productCode",
                product.getProductCode()
        );

        data.put(
                "productCodeForScheme",
                product.getProductCodeForScheme()
        );

        data.put(
                "visible",
                product.getIsVisible()
        );

        data.put(
                "active",
                product.getActive()
        );

        data.put(
                "blocked",
                product.getBlocked()
        );

        data.put(
                "transactionType",
                product.getTransactionType()
        );

        data.put(
                "commissionApplicable",
                product.getIsCommissions()
        );

        data.put(
                "commissionType",
                product.getSelectIsCommission()
        );

        data.put(
                "settlement",
                product.getIsSettlement()
        );

        data.put(
                "hasCharges",
                product.getHasCharges()
        );

        data.put(
                "reverseTransaction",
                product.getReverseTx()
        );

        data.put(
                "clusterMode",
                product.getClusterMode()
        );

        data.put(
                "minimumPrepaidBalance",
                product.getMinimumPrepaidBalance()
        );

        data.put(
                "minimumPostpaidBalance",
                product.getMinimumPostpaidBalance()
        );

        data.put(
                "minimumGullakBalance",
                product.getMinimumGullakBalance()
        );

        data.put(
                "minimumSingleTransactionAmount",
                product.getMinimumSingleTransactionAmount()
        );

        data.put(
                "maximumSingleTransactionAmount",
                product.getMaximumSingleTransactionAmount()
        );

        data.put("sgst", product.getSgst());
        data.put("tds", product.getTds());
        data.put("priority", product.getPriority());

        data.put(
                "productGroup",
                product.getProductGroup()
        );

        data.put(
                "dashboardUrl",
                product.getDashboardUrl()
        );

        data.put(
                "dashboardUrlForApk",
                product.getDashboardUrlForApk()
        );

        data.put(
                "dashboardImage",
                product.getDashboardImage()
        );

        return data;
    }

    private Map<String, Object> mapSubProduct(
            String allocationSubProductCode,
            SubProducts subProduct
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "allocatedSubProductCode",
                allocationSubProductCode
        );

        if (subProduct == null) {

            data.put(
                    "masterRecordFound",
                    false
            );

            return data;
        }

        data.put("masterRecordFound", true);
        data.put("id", subProduct.getId());

        data.put(
                "title",
                subProduct.getSubProductTitle()
        );

        data.put(
                "subProductCode",
                subProduct.getSubProductCode()
        );

        data.put(
                "walletType",
                subProduct.getWalletType()
        );

        data.put(
                "settlement",
                subProduct.getIsSettlement()
        );

        data.put(
                "settlementTime",
                subProduct.getSettlementTime()
        );

        data.put(
                "active",
                subProduct.getIsActive()
        );

        data.put(
                "visible",
                subProduct.getIsVisible()
        );

        data.put(
                "blocked",
                subProduct.getIsBlocked()
        );

        data.put(
                "whiteLabelAdmin",
                subProduct.getIsWhiteLabelAdmin()
        );

        data.put(
                "whiteLabelAdminId",
                subProduct.getWhiteLabelAdminId()
        );

        if (subProduct.getProduct() != null) {

            data.put(
                    "parentProductId",
                    subProduct.getProduct().getId()
            );

            data.put(
                    "parentProductCode",
                    subProduct
                            .getProduct()
                            .getProductCode()
            );

            data.put(
                    "parentProductName",
                    subProduct
                            .getProduct()
                            .getName()
            );
        }

        return data;
    }

    private Map<String, Object> mapScheme(
            String allocationSchemeCode,
            Scheme scheme
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "allocatedSchemeCode",
                allocationSchemeCode
        );

        if (scheme == null) {

            data.put(
                    "masterRecordFound",
                    false
            );

            return data;
        }

        data.put("masterRecordFound", true);
        data.put("id", scheme.getId());

        data.put(
                "schemeName",
                scheme.getSchemeName()
        );

        data.put(
                "schemeCode",
                scheme.getSchemeCode()
        );

        data.put(
                "schemeCombinationHashCode",
                scheme.getSchemeCombinationHashCode()
        );

        data.put(
                "active",
                scheme.getIsActive()
        );

        data.put(
                "createdOn",
                scheme.getCreatedOn()
        );

        data.put(
                "createdBy",
                scheme.getCreatedBy()
        );

        data.put(
                "editedOn",
                scheme.getEditedOn()
        );

        data.put(
                "editedBy",
                scheme.getEditedBy()
        );

        return data;
    }

    private Map<String, Object> mapRetailer(
            Retailer entity
    ) {

        Map<String, Object> data =
                createCommonHierarchyInformation(
                        "RT",
                        entity.getRetailerId(),
                        entity.getParentAccount(),
                        entity.getTitle(),
                        entity.getName(),
                        entity.getMobileNumber(),
                        entity.getEmailId(),
                        entity.getDob(),
                        entity.getStatus(),
                        entity.getRejectionReason(),
                        entity.getRemarks(),
                        entity.getAddress(),
                        entity.getOutletAddress()
                );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put(
                "verification",
                buildVerificationInformation(
                        entity.getEmailVerified(),
                        entity.getPanVerified(),
                        entity.getIsAadhaarVerified(),
                        entity.getAadhaarPanVerified(),
                        entity.getGstNumberVerified()
                )
        );

        data.put(
                "identification",
                buildIdentificationInformation(
                        entity.getAadhaarNumber(),
                        entity.getPanNumber(),
                        entity.getShopNumber(),
                        entity.getBusinessNumber()
                )
        );

        data.put(
                "documents",
                buildDocumentInformation(
                        entity.getAadhaarCardFrontImage(),
                        entity.getAadhaarCardBackImage(),
                        entity.getPanCardImage(),
                        entity.getShopImage(),
                        entity.getBusinessProofImage()
                )
        );

        data.put(
                "createdOn",
                entity.getCreatedOn()
        );

        data.put(
                "createdBy",
                entity.getCreatedBy()
        );

        data.put(
                "lastEditedOn",
                entity.getLastEditedOn()
        );

        data.put(
                "lastEditedBy",
                entity.getEditedBy()
        );

        data.put(
                "createdByIpAddress",
                entity.getCreatedByIPAddress()
        );

        data.put(
                "latitude",
                entity.getLatitude()
        );

        data.put(
                "longitude",
                entity.getLongitude()
        );

        return data;
    }

    private Map<String, Object> mapDistributor(
            Distributor entity
    ) {

        Map<String, Object> data =
                createCommonHierarchyInformation(
                        "DT",
                        entity.getDistributorId(),
                        entity.getParentAccount(),
                        entity.getTitle(),
                        entity.getName(),
                        entity.getMobileNumber(),
                        entity.getEmailId(),
                        entity.getDob(),
                        entity.getStatus(),
                        entity.getRejectionReason(),
                        entity.getRemarks(),
                        entity.getAddress(),
                        entity.getOutletAddress()
                );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put(
                "verification",
                buildVerificationInformation(
                        entity.getEmailVerified(),
                        entity.getPanVerified(),
                        entity.getIsAadhaarVerified(),
                        entity.getAadhaarPanVerified(),
                        entity.getGstNumberVerified()
                )
        );

        data.put(
                "identification",
                buildIdentificationInformation(
                        entity.getAadhaarNumber(),
                        entity.getPanNumber(),
                        entity.getShopNumber(),
                        entity.getBusinessNumber()
                )
        );

        data.put(
                "documents",
                buildDocumentInformation(
                        entity.getAadhaarCardFrontImage(),
                        entity.getAadhaarCardBackImage(),
                        entity.getPanCardImage(),
                        entity.getShopImage(),
                        entity.getBusinessProofImage()
                )
        );

        data.put("createdOn", entity.getCreatedOn());
        data.put("createdBy", entity.getCreatedBy());

        data.put(
                "lastEditedOn",
                entity.getLastEditedOn()
        );

        data.put(
                "lastEditedBy",
                entity.getEditedBy()
        );

        return data;
    }

    private Map<String, Object> mapPartner(
            Partner entity
    ) {

        Map<String, Object> data =
                createCommonHierarchyInformation(
                        "PT",
                        entity.getPartnerId(),
                        entity.getParentAccount(),
                        entity.getTitle(),
                        entity.getName(),
                        entity.getMobileNumber(),
                        entity.getEmailId(),
                        entity.getDob(),
                        entity.getStatus(),
                        entity.getRejectionReason(),
                        entity.getRemarks(),
                        entity.getAddress(),
                        entity.getOutletAddress()
                );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put(
                "verification",
                buildVerificationInformation(
                        entity.getEmailVerified(),
                        entity.getPanVerified(),
                        entity.getIsAadhaarVerified(),
                        entity.getAadhaarPanVerified(),
                        entity.getGstNumberVerified()
                )
        );

        data.put(
                "identification",
                buildIdentificationInformation(
                        entity.getAadhaarNumber(),
                        entity.getPanNumber(),
                        entity.getShopNumber(),
                        entity.getBusinessNumber()
                )
        );

        data.put(
                "documents",
                buildDocumentInformation(
                        entity.getAadhaarCardFrontImage(),
                        entity.getAadhaarCardBackImage(),
                        entity.getPanCardImage(),
                        entity.getShopImage(),
                        entity.getBusinessProofImage()
                )
        );

        data.put("createdOn", entity.getCreatedOn());
        data.put("createdBy", entity.getCreatedBy());

        return data;
    }

    private Map<String, Object> mapSuperDistributor(
            SuperDistributor entity
    ) {

        Map<String, Object> data =
                createCommonHierarchyInformation(
                        "SD",
                        entity.getSuperDistributorId(),
                        entity.getParentAccount(),
                        entity.getTitle(),
                        entity.getName(),
                        entity.getMobileNumber(),
                        entity.getEmailId(),
                        entity.getDob(),
                        entity.getStatus(),
                        entity.getRejectionReason(),
                        entity.getRemarks(),
                        entity.getAddress(),
                        entity.getOutletAddress()
                );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put(
                "verification",
                buildVerificationInformation(
                        entity.getEmailVerified(),
                        entity.getPanVerified(),
                        entity.getIsAadhaarVerified(),
                        entity.getAadhaarPanVerified(),
                        entity.getGstNumberVerified()
                )
        );

        data.put(
                "identification",
                buildIdentificationInformation(
                        entity.getAadhaarNumber(),
                        entity.getPanNumber(),
                        entity.getShopNumber(),
                        entity.getBusinessNumber()
                )
        );

        data.put(
                "documents",
                buildDocumentInformation(
                        entity.getAadhaarCardFrontImage(),
                        entity.getAadhaarCardBackImage(),
                        entity.getPanCardImage(),
                        entity.getShopImage(),
                        entity.getBusinessProofImage()
                )
        );

        data.put("createdOn", entity.getCreatedOn());
        data.put("createdBy", entity.getCreatedBy());

        return data;
    }

    private Map<String, Object> mapWhiteLabel(
            WhiteLabels entity
    ) {

        Map<String, Object> data =
                createCommonHierarchyInformation(
                        "WL",
                        entity.getWhiteLabelId(),
                        entity.getParentAccount(),
                        entity.getTitle(),
                        entity.getName(),
                        entity.getMobileNumber(),
                        entity.getEmailId(),
                        entity.getDob(),
                        entity.getStatus(),
                        entity.getRejectionReason(),
                        entity.getRemarks(),
                        entity.getAddress(),
                        entity.getOutletAddress()
                );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put(
                "businessDescription",
                entity.getBusinessDesc()
        );

        data.put(
                "domainUrl",
                entity.getDomainURL()
        );

        data.put(
                "admin",
                entity.getIsAdmin()
        );

        data.put(
                "verification",
                buildVerificationInformation(
                        entity.getEmailVerified(),
                        entity.getPanVerified(),
                        entity.getIsAadhaarVerified(),
                        entity.getAadhaarPanVerified(),
                        entity.getGstNumberVerified()
                )
        );

        data.put(
                "identification",
                buildIdentificationInformation(
                        entity.getAadhaarNumber(),
                        entity.getPanNumber(),
                        entity.getShopNumber(),
                        entity.getBusinessNumber()
                )
        );

        data.put(
                "documents",
                buildDocumentInformation(
                        entity.getAadhaarCardFrontImage(),
                        entity.getAadhaarCardBackImage(),
                        entity.getPanCardImage(),
                        entity.getShopImage(),
                        entity.getBusinessProofImage()
                )
        );

        data.put("createdOn", entity.getCreatedOn());
        data.put("createdBy", entity.getCreatedBy());

        return data;
    }

    private Map<String, Object> mapAdmin(
            Admin entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("hierarchyType", "AD");
        data.put("hierarchyId", entity.getId());
        data.put("name", entity.getName());

        data.put(
                "mobileNumber",
                entity.getMobileNumber()
        );

        data.put(
                "emailId",
                entity.getEmailId()
        );

        data.put(
                "emailVerified",
                entity.getEmailVerified()
        );

        data.put(
                "userName",
                entity.getUserName()
        );

        data.put(
                "status",
                entity.getStatus()
        );

        data.put(
                "rejectionReason",
                entity.getRejectionReason()
        );

        data.put(
                "remarks",
                entity.getRemarks()
        );

        data.put(
                "loginUrl",
                entity.getLoginURL()
        );

        return data;
    }

    private Map<String, Object> mapAdminUser(
            AdminUser entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("hierarchyType", "AU");
        data.put("hierarchyId", entity.getId());

        data.put(
                "parentAccount",
                entity.getParentAccount()
        );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put("name", entity.getName());

        data.put(
                "mobileNumber",
                entity.getMobileNumber()
        );

        data.put(
                "username",
                entity.getUsername()
        );

        data.put(
                "emailId",
                entity.getEmailId()
        );

        data.put(
                "emailVerified",
                entity.getEmailVerified()
        );

        data.put("dob", entity.getDob());
        data.put("status", entity.getStatus());

        data.put(
                "address",
                mapAddress(entity.getAdd())
        );

        data.put(
                "permission",
                entity.getPermission()
        );

        data.put(
                "createdOn",
                entity.getCreatedOn()
        );

        data.put(
                "createdBy",
                entity.getCreatedBy()
        );

        return data;
    }

    private Map<String, Object> mapWhiteLabelUser(
            WhiteLabelUser entity
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("hierarchyType", "WLAU");
        data.put("hierarchyId", entity.getId());

        data.put(
                "parentAccount",
                entity.getParentAccount()
        );

        data.put(
                "abbreviation",
                entity.getAbbreviation()
        );

        data.put("name", entity.getName());

        data.put(
                "mobileNumber",
                entity.getMobileNumber()
        );

        data.put(
                "username",
                entity.getUsername()
        );

        data.put(
                "emailId",
                entity.getEmailId()
        );

        data.put(
                "emailVerified",
                entity.getEmailVerified()
        );

        data.put("dob", entity.getDob());
        data.put("status", entity.getStatus());

        data.put(
                "address",
                mapAddress(entity.getAdd())
        );

        data.put(
                "permission",
                entity.getPermission()
        );

        data.put(
                "createdOn",
                entity.getCreatedOn()
        );

        data.put(
                "createdBy",
                entity.getCreatedBy()
        );

        return data;
    }

    private Map<String, Object>
    createCommonHierarchyInformation(
            String hierarchyType,
            String hierarchyId,
            String parentAccount,
            String title,
            String name,
            String mobileNumber,
            String emailId,
            Object dob,
            Object status,
            String rejectionReason,
            String remarks,
            Address mainAddress,
            Address outletAddress
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("hierarchyType", hierarchyType);
        data.put("hierarchyId", hierarchyId);
        data.put("parentAccount", parentAccount);
        data.put("title", title);
        data.put("name", name);
        data.put("mobileNumber", mobileNumber);
        data.put("emailId", emailId);
        data.put("dob", dob);
        data.put("status", status);

        data.put(
                "rejectionReason",
                rejectionReason
        );

        data.put("remarks", remarks);

        data.put(
                "mainAddress",
                mapAddress(mainAddress)
        );

        data.put(
                "outletAddress",
                mapAddress(outletAddress)
        );

        return data;
    }

    private Map<String, Object>
    buildVerificationInformation(
            Boolean emailVerified,
            Boolean panVerified,
            Boolean aadhaarVerified,
            Boolean aadhaarPanVerified,
            Boolean gstVerified
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "emailVerified",
                Boolean.TRUE.equals(emailVerified)
        );

        data.put(
                "panVerified",
                Boolean.TRUE.equals(panVerified)
        );

        data.put(
                "aadhaarVerified",
                Boolean.TRUE.equals(aadhaarVerified)
        );

        data.put(
                "aadhaarPanVerified",
                Boolean.TRUE.equals(
                        aadhaarPanVerified
                )
        );

        data.put(
                "gstVerified",
                Boolean.TRUE.equals(gstVerified)
        );

        return data;
    }

    private Map<String, Object>
    buildIdentificationInformation(
            String aadhaarNumber,
            String panNumber,
            String shopNumber,
            String businessNumber
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        /*
         * Mask Aadhaar in the API response.
         */
        data.put(
                "aadhaarNumber",
                maskAadhaar(aadhaarNumber)
        );

        data.put("panNumber", panNumber);
        data.put("shopNumber", shopNumber);

        data.put(
                "businessNumber",
                businessNumber
        );

        return data;
    }

    private Map<String, Object>
    buildDocumentInformation(
            String aadhaarFront,
            String aadhaarBack,
            String panCard,
            String shopImage,
            String businessProof
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "aadhaarCardFrontImage",
                aadhaarFront
        );

        data.put(
                "aadhaarCardBackImage",
                aadhaarBack
        );

        data.put(
                "panCardImage",
                panCard
        );

        data.put(
                "shopImage",
                shopImage
        );

        data.put(
                "businessProofImage",
                businessProof
        );

        return data;
    }

    private Map<String, Object> mapAddress(
            Address address
    ) {

        if (address == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "outletName",
                address.getOutletName()
        );

        data.put(
                "companyType",
                address.getCompanyType()
        );

        data.put(
                "companyName",
                address.getCompanyName()
        );

        data.put(
                "address",
                address.getAddress()
        );

        data.put("city", address.getCity());
        data.put("state", address.getState());

        data.put(
                "zipCode",
                address.getZipCode()
        );

        return data;
    }

    private EntityNotFoundException notFound(
            String hierarchyType,
            String hierarchyId
    ) {

        return new EntityNotFoundException(
                hierarchyType +
                        " record not found for hierarchyId: " +
                        hierarchyId
        );
    }

    private String trimToNull(String value) {

        if (value == null ||
                value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String maskAadhaar(
            String aadhaarNumber
    ) {

        if (aadhaarNumber == null ||
                aadhaarNumber.isBlank()) {
            return null;
        }

        if (aadhaarNumber.length() <= 4) {
            return aadhaarNumber;
        }

        return "XXXXXXXX" +
                aadhaarNumber.substring(
                        aadhaarNumber.length() - 4
                );
    }

    private Map<String, Object> getUnderHierarchyInformation(
            String hierarchyId
    ) {

        log.info(
                "Fetching descendants for hierarchyId: {}",
                hierarchyId
        );

        Map<String, Object> result =
                new LinkedHashMap<>();

        boolean hierarchyNodeExists =
                accessHierarchyNodeRepository
                        .existsById(hierarchyId);

        if (!hierarchyNodeExists) {

            log.warn(
                    "Hierarchy node not found in access_hierarchy_node for hierarchyId: {}",
                    hierarchyId
            );

            result.put("available", false);
            result.put("totalCount", 0);
            result.put("directChildrenCount", 0);
            result.put("maximumDepth", 0);

            result.put(
                    "message",
                    "Hierarchy node is not available in access hierarchy"
            );

            result.put(
                    "items",
                    Collections.emptyList()
            );

            return result;
        }

        List<UnderHierarchyProjection> descendants =
                accessHierarchyPathRepository
                        .findAllDescendants(hierarchyId);

        List<Map<String, Object>> items =
                descendants.stream()
                        .map(this::mapUnderHierarchyNode)
                        .toList();

        long directChildrenCount =
                descendants.stream()
                        .filter(item ->
                                Integer.valueOf(1)
                                        .equals(item.getDepth())
                        )
                        .count();

        int maximumDepth =
                descendants.stream()
                        .map(
                                UnderHierarchyProjection::
                                        getDepth
                        )
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);

        result.put("available", true);

        result.put(
                "rootHierarchyId",
                hierarchyId
        );

        result.put(
                "totalCount",
                items.size()
        );

        result.put(
                "directChildrenCount",
                directChildrenCount
        );

        result.put(
                "maximumDepth",
                maximumDepth
        );

        result.put("items", items);

        log.info(
                "Fetched {} descendants for hierarchyId: {}",
                items.size(),
                hierarchyId
        );

        return result;
    }

    private Map<String, Object> mapUnderHierarchyNode(
            UnderHierarchyProjection projection
    ) {

        Map<String, Object> item =
                new LinkedHashMap<>();

        item.put(
                "hierarchyId",
                projection.getHierarchyId()
        );

        item.put(
                "hierarchyType",
                projection.getHierarchyType()
        );

        item.put(
                "parentHierarchyId",
                projection.getParentHierarchyId()
        );

        item.put(
                "depth",
                projection.getDepth()
        );

        item.put(
                "relationship",
                getHierarchyRelationship(
                        projection.getDepth()
                )
        );

        item.put(
                "createdOn",
                projection.getCreatedOn()
        );

        return item;
    }

    private String getHierarchyRelationship(
            Integer depth
    ) {

        if (depth == null) {
            return "UNKNOWN";
        }

        return switch (depth) {

            case 0 -> "SELF";

            case 1 -> "DIRECT_CHILD";

            default -> "DESCENDANT";
        };
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getGroupedHierarchyUnderLogin() {

        long startTime = System.currentTimeMillis();

        String loginHierarchyId = tokenUtil.extractHierarchyId();

        log.info(
                "[GROUPED HIERARCHY] START loginHierarchyId={}",
                loginHierarchyId
        );

        AccessHierarchyNode loginNode =
                accessHierarchyNodeRepository
                        .findById(loginHierarchyId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Hierarchy not found: " + loginHierarchyId
                                )
                        );

        String loginHierarchyType =
                loginNode.getHierarchyType() == null
                        ? ""
                        : loginNode.getHierarchyType().trim().toUpperCase();

        /*
         * Default ancestor is the logged-in hierarchy.
         *
         * For WLAU, use its parent WL because WLAU works on behalf
         * of the White Label.
         */
        String ancestorHierarchyId = loginHierarchyId;

        if (
                loginHierarchyType.endsWith("AU")
                        && loginNode.getParentHierarchyId() != null
                        && loginNode
                        .getParentHierarchyId()
                        .contains("-WL-")
        ) {

            ancestorHierarchyId =
                    loginNode.getParentHierarchyId().trim();

            log.info(
                    "[GROUPED HIERARCHY] WLAU detected. loginHierarchyId={}, parentWL={}",
                    loginHierarchyId,
                    ancestorHierarchyId
            );
        }

        List<String> descendantIds =
                new ArrayList<>(
                        accessHierarchyPathRepository
                                .findDescendantsByAncestor(
                                        ancestorHierarchyId
                                )
                );

        /*
         * Remove the logged-in hierarchy itself.
         *
         * Example:
         * When SCS-WL-001 logs in, SCS-WL-001 should not
         * appear in the result.
         */
        descendantIds.removeIf(
                hierarchyId ->
                        hierarchyId == null
                                || hierarchyId.equalsIgnoreCase(loginHierarchyId)
        );

        /*
         * For WLAU, also remove the parent WL itself.
         *
         * The response should only contain users below the WL.
         */
        String finalAncestorHierarchyId = ancestorHierarchyId;

        descendantIds.removeIf(
                hierarchyId ->
                        hierarchyId.equalsIgnoreCase(
                                finalAncestorHierarchyId
                        )
        );

        if (descendantIds.isEmpty()) {

            log.info(
                    "[GROUPED HIERARCHY] No descendants found. loginHierarchyId={}",
                    loginHierarchyId
            );

            return new LinkedHashMap<>();
        }

        List<UserDetails> hierarchyUsers =
                userDetailsRepository
                        .findActiveUsersByHierarchyIds(
                                descendantIds
                        );

        /*
         * Hierarchy codes to return.
         *
         * Add or remove codes here according to your requirement.
         */
        Set<String> allowedHierarchyCodes =
                Set.of(
                        "WL",
                        "SD",
                        "PT",
                        "DT",
                        "RT"
                );

        /*
         * LinkedHashMap keeps the hierarchy order.
         */
        Map<String, List<String>> groupedResponse =
                new LinkedHashMap<>();

        List<String> hierarchyOrder =
                List.of(
                        "WL",
                        "SD",
                        "PT",
                        "DT",
                        "RT"
                );

        Map<String, List<String>> groupedUsers =
                hierarchyUsers
                        .stream()
                        .filter(user ->
                                user.getHierarchyCode() != null
                                        && user.getHierarchyId() != null
                        )
                        .filter(user ->
                                allowedHierarchyCodes.contains(
                                        user.getHierarchyCode()
                                                .trim()
                                                .toUpperCase()
                                )
                        )
                        .collect(
                                Collectors.groupingBy(
                                        user ->
                                                user.getHierarchyCode()
                                                        .trim()
                                                        .toUpperCase(),
                                        Collectors.mapping(
                                                UserDetails::getHierarchyId,
                                                Collectors.toList()
                                        )
                                )
                        );

        /*
         * Add only hierarchy groups that contain records.
         */
        for (String hierarchyCode : hierarchyOrder) {

            List<String> hierarchyIds =
                    groupedUsers.get(hierarchyCode);

            if (hierarchyIds != null && !hierarchyIds.isEmpty()) {

                groupedResponse.put(
                        hierarchyCode,
                        hierarchyIds
                                .stream()
                                .distinct()
                                .sorted()
                                .toList()
                );
            }
        }

        log.info(
                "[GROUPED HIERARCHY] SUCCESS loginHierarchyId={}, " +
                        "ancestorHierarchyId={}, groups={}, totalUsers={}, timeTaken={}ms",
                loginHierarchyId,
                ancestorHierarchyId,
                groupedResponse.keySet(),
                hierarchyUsers.size(),
                System.currentTimeMillis() - startTime
        );

        return groupedResponse;
    }


    @Transactional(readOnly = true)
    public List<HierarchyOptionResponse> getHierarchyOptions(
            String hierarchyCode
    ) {

        String requestedHierarchyCode =
                normalizeHierarchyCodeOption(hierarchyCode);

        validateRequestedHierarchyCode(
                requestedHierarchyCode
        );

        String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy ID is missing from token"
            );
        }

        loginHierarchyId =
                loginHierarchyId.trim();

        String loginHierarchyType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        if (loginHierarchyType == null
                || loginHierarchyType.isBlank()) {

            throw new IllegalArgumentException(
                    "Unable to identify hierarchy type for login user: "
                            + loginHierarchyId
            );
        }

        loginHierarchyType =
                loginHierarchyType.trim().toUpperCase();

        log.info(
                "Fetching hierarchy options | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "requestedHierarchyCode={}",
                loginHierarchyId,
                loginHierarchyType,
                requestedHierarchyCode
        );

        List<AccessHierarchyNode> hierarchyNodes;

        /*
         * AD and AU have complete system access.
         *
         * Example:
         * SCS-AD-001 can see all RT users.
         * AU-AU-006 can also see all RT users.
         */
        if (commonMethods.hasFullSystemAccess(loginHierarchyType)) {

            hierarchyNodes =
                    accessHierarchyNodeRepository
                            .findAllByHierarchyType(
                                    requestedHierarchyCode
                            );

            log.info(
                    "Full hierarchy option access granted | "
                            + "loginHierarchyId={} | "
                            + "requestedHierarchyCode={} | "
                            + "resultCount={}",
                    loginHierarchyId,
                    requestedHierarchyCode,
                    hierarchyNodes.size()
            );

        } else {

            /*
             * For WLAU, this resolves its parent WL.
             *
             * SCS-WLAU-001 -> SCS-WL-001
             * LPS-WLAU-001 -> LPS-WL-001
             *
             * Other hierarchy users use their own hierarchy ID.
             */
            String effectiveHierarchyId =
                    resolveEffectiveHierarchyId(
                            loginHierarchyId,
                            loginHierarchyType
                    );

            hierarchyNodes =
                    accessHierarchyNodeRepository
                            .findByTypeUnderHierarchy(
                                    effectiveHierarchyId,
                                    requestedHierarchyCode
                            );

            log.info(
                    "Hierarchy-scoped options fetched | "
                            + "loginHierarchyId={} | "
                            + "loginHierarchyType={} | "
                            + "effectiveHierarchyId={} | "
                            + "requestedHierarchyCode={} | "
                            + "resultCount={}",
                    loginHierarchyId,
                    loginHierarchyType,
                    effectiveHierarchyId,
                    requestedHierarchyCode,
                    hierarchyNodes.size()
            );
        }

        return hierarchyNodes
                .stream()
                .map(node ->
                        HierarchyOptionResponse.builder()
                                .hierarchyId(
                                        node.getHierarchyId()
                                )
                                .hierarchyCode(
                                        node.getHierarchyType()
                                )
                                .build()
                )
                .toList();
    }

    public String resolveEffectiveHierarchyId(
            String loginHierarchyId,
            String loginHierarchyType
    ) {

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy ID is required"
            );
        }

        if (loginHierarchyType == null
                || loginHierarchyType.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy type is required"
            );
        }

        final String normalizedLoginHierarchyId =
                loginHierarchyId.trim();

        final String normalizedLoginType =
                loginHierarchyType
                        .trim()
                        .toUpperCase();

        final String expectedParentType;

        switch (normalizedLoginType) {

            case "AU" -> expectedParentType = "AD";

            case "WLAU" -> expectedParentType = "WL";

            default -> {
                // AD, WL, SD, PT, DT, RT, ET use their own tree.
                return normalizedLoginHierarchyId;
            }
        }

        AccessHierarchyNode delegatedUserNode =
                accessHierarchyNodeRepository
                        .findById(normalizedLoginHierarchyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Hierarchy node not found: "
                                                + normalizedLoginHierarchyId
                                )
                        );

        if (!normalizedLoginType.equalsIgnoreCase(
                delegatedUserNode.getHierarchyType()
        )) {

            throw new IllegalStateException(
                    "Hierarchy type mismatch. "
                            + "Hierarchy ID: "
                            + normalizedLoginHierarchyId
                            + ", expected type: "
                            + normalizedLoginType
                            + ", actual type: "
                            + delegatedUserNode.getHierarchyType()
            );
        }

        String rawParentHierarchyId =
                delegatedUserNode.getParentHierarchyId();

        if (rawParentHierarchyId == null
                || rawParentHierarchyId.isBlank()
                || "-".equals(rawParentHierarchyId.trim())) {

            throw new IllegalStateException(
                    "Parent hierarchy is not configured for: "
                            + normalizedLoginHierarchyId
            );
        }

        final String parentHierarchyId =
                rawParentHierarchyId.trim();

        AccessHierarchyNode parentNode =
                accessHierarchyNodeRepository
                        .findById(parentHierarchyId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Parent hierarchy node not found. "
                                                + "Login hierarchy: "
                                                + normalizedLoginHierarchyId
                                                + ", parent hierarchy: "
                                                + parentHierarchyId
                                )
                        );

        if (!expectedParentType.equalsIgnoreCase(
                parentNode.getHierarchyType()
        )) {

            throw new IllegalStateException(
                    "Invalid delegated hierarchy relationship. "
                            + "Login hierarchy: "
                            + normalizedLoginHierarchyId
                            + ", login type: "
                            + normalizedLoginType
                            + ", parent hierarchy: "
                            + parentHierarchyId
                            + ", expected parent type: "
                            + expectedParentType
                            + ", actual parent type: "
                            + parentNode.getHierarchyType()
            );
        }

        log.info(
                "Effective hierarchy resolved | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "effectiveHierarchyId={} | "
                        + "effectiveHierarchyType={}",
                normalizedLoginHierarchyId,
                normalizedLoginType,
                parentHierarchyId,
                expectedParentType
        );

        return parentHierarchyId;
    }

    private String normalizeHierarchyCode(
            String hierarchyCode
    ) {

        if (hierarchyCode == null
                || hierarchyCode.isBlank()) {

            throw new IllegalArgumentException(
                    "Hierarchy code is required"
            );
        }

        return hierarchyCode
                .trim()
                .toUpperCase();
    }

    private void validateRequestedHierarchyCode(
            String hierarchyCode
    ) {

        if (!ALLOWED_HIERARCHY_CODES.contains(
                hierarchyCode
        )) {

            throw new IllegalArgumentException(
                    "Invalid hierarchy code: "
                            + hierarchyCode
                            + ". Allowed values are: "
                            + ALLOWED_HIERARCHY_CODES
            );
        }
    }

}
