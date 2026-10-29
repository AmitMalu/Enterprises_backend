package com.scs.admin.service.impl;


import com.scs.admin.dto.SchemeAllocationDto;
import com.scs.admin.dto.SchemeAllocationRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.exception.ResourceNotFoundException;
import com.scs.admin.repositories.*;
import com.scs.admin.request.SchemeAllocationRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.RetailerSchemeAllocationResponse;
import com.scs.admin.response.SchemeDropdownListBySubProductResponse;
import com.scs.admin.response.UserSearchResponseDto;
import com.scs.admin.service.SchemeAllocationService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchemeAllocationServiceImpl implements SchemeAllocationService {

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

//    @Override
//    @Transactional
//    public List<SchemeAllocation> assignOrUpdateSchemes(SchemeAllocationRequestDto request) {
//        List<SchemeAllocation> result = new ArrayList<>();
//
//        for (SchemeAllocationDto dto : request.getCodes()) {
//            // 1. Find product by subproduct
//            SubProducts subProduct = subProductsRepository.findBySubProductCode(dto.getSubProductCode())
//                    .orElseThrow(() -> new ResourceNotFoundException("SubProduct not found: " + dto.getSubProductCode()));
//
//            String productCode = subProduct.getProduct().getProductCode();
//
//            // 2. Check if record already exists
//            Optional<SchemeAllocation> existing = schemeAllocationRepository
//                    .findByHierarchyIdAndProductCodeAndSubProductCode(
//                            request.getHierarchyId(), productCode, dto.getSubProductCode()
//                    );
//
//            SchemeAllocation allocation;
//            if (existing.isPresent()) {
//                // Update
//                allocation = existing.get();
//                allocation.setSchemeCode(dto.getSchemeCode());
//            } else {
//                // Insert
//                allocation = new SchemeAllocation();
//                allocation.setHierarchyId(request.getHierarchyId());
//                allocation.setProductCode(productCode);
//                allocation.setSubProductCode(dto.getSubProductCode());
//                allocation.setSchemeCode(dto.getSchemeCode());
//            }
//
//            result.add(allocation);
//        }
//
//        // Save all at once (JPA handles insert/update automatically)
//        return schemeAllocationRepository.saveAll(result);
//    }

    @Override
    @Transactional
    public List<SchemeAllocation> assignOrUpdateSchemes(
            SchemeAllocationRequestDto request
    ) {

        log.info(
                "Starting scheme allocation. hierarchyId={}, requestedSubProducts={}",
                request.getHierarchyId(),
                request.getCodes() != null ? request.getCodes().size() : 0
        );

        validateSchemeAllocationRequest(request);

        String targetHierarchyId = request.getHierarchyId().trim();

        List<SchemeAllocation> result = new ArrayList<>();

        for (SchemeAllocationDto dto : request.getCodes()) {

            if (dto == null) {
                throw new IllegalArgumentException(
                        "Scheme allocation details cannot be null."
                );
            }

            String subProductCode = normalizeRequiredValue(
                    dto.getSubProductCode(),
                    "Subproduct code is required."
            );

            String schemeCode = normalizeRequiredValue(
                    dto.getSchemeCode(),
                    "Scheme code is required for subproduct: " + subProductCode
            );

            log.info(
                    "Processing scheme allocation. hierarchyId={}, " +
                            "subProductCode={}, schemeCode={}",
                    targetHierarchyId,
                    subProductCode,
                    schemeCode
            );

            /*
             * Step 1: Find the subproduct.
             */
            SubProducts subProduct = subProductsRepository
                    .findBySubProductCode(subProductCode)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "SubProduct not found: " + subProductCode
                            )
                    );

            /*
             * Step 2: Validate whether the target hierarchy belongs to the
             * White Label currently assigned to this subproduct.
             */
            validateHierarchyCanReceiveSubProduct(
                    targetHierarchyId,
                    subProduct
            );

            /*
             * Step 3: Get product code from subproduct.
             */
            if (subProduct.getProduct() == null
                    || subProduct.getProduct().getProductCode() == null
                    || subProduct.getProduct().getProductCode().isBlank()) {

                throw new IllegalStateException(
                        "Product configuration is missing for subproduct: "
                                + subProductCode
                );
            }

            String productCode =
                    subProduct.getProduct().getProductCode().trim();

            /*
             * Step 4: Find existing scheme allocation.
             */
            Optional<SchemeAllocation> existing =
                    schemeAllocationRepository
                            .findByHierarchyIdAndProductCodeAndSubProductCode(
                                    targetHierarchyId,
                                    productCode,
                                    subProductCode
                            );

            SchemeAllocation allocation;

            if (existing.isPresent()) {

                allocation = existing.get();

                log.info(
                        "Existing scheme allocation found. Updating scheme. " +
                                "allocationId={}, hierarchyId={}, " +
                                "subProductCode={}, oldSchemeCode={}, newSchemeCode={}",
                        allocation.getId(),
                        targetHierarchyId,
                        subProductCode,
                        allocation.getSchemeCode(),
                        schemeCode
                );

                allocation.setSchemeCode(schemeCode);

            } else {

                log.info(
                        "Creating new scheme allocation. hierarchyId={}, " +
                                "productCode={}, subProductCode={}, schemeCode={}",
                        targetHierarchyId,
                        productCode,
                        subProductCode,
                        schemeCode
                );

                allocation = new SchemeAllocation();
                allocation.setHierarchyId(targetHierarchyId);
                allocation.setProductCode(productCode);
                allocation.setSubProductCode(subProductCode);
                allocation.setSchemeCode(schemeCode);
            }

            result.add(allocation);
        }

        List<SchemeAllocation> savedAllocations =
                schemeAllocationRepository.saveAll(result);

        log.info(
                "Scheme allocation completed successfully. hierarchyId={}, " +
                        "savedRecords={}",
                targetHierarchyId,
                savedAllocations.size()
        );

        return savedAllocations;
    }

    @Override
    public CommonResponse<String> saveSchemeAllocation(SchemeAllocationRequest request) {

        boolean exists = schemeAllocationRepository
                .existsByHierarchyIdAndSchemeCodeAndProductCodeAndSubProductCode(
                        request.getHierarchyId(),
                        request.getSchemeCode(),
                        request.getProductCode(),
                        request.getSubProductCode());

        if (exists) {
            return CommonResponse.error(409, "Scheme allocation already exists for given parameters.");
        }

        SchemeAllocation allocation = new SchemeAllocation();
        allocation.setHierarchyId(request.getHierarchyId());
        allocation.setProductCode(request.getProductCode());
        allocation.setSubProductCode(request.getSubProductCode());
        allocation.setSchemeCode(request.getSchemeCode());

        schemeAllocationRepository.save(allocation);

        return CommonResponse.success(null, "Scheme allocation saved successfully.");
    }

    @Override
    public List<SchemeDropdownListBySubProductResponse> getSchemeDropdownListBySubProduct(String subProductName) {

        SubProducts subProduct = subProductsRepository.findBySubProductTitle(subProductName)
                .orElseThrow(() -> new RuntimeException("Sub Product Not found"));

        String subProductId = subProduct.getId();

        List<Object[]> rawResults = schemeRepository.findActiveSchemesBySubProductId(subProductId);

        return rawResults.stream()
                .map(obj -> new SchemeDropdownListBySubProductResponse(
                        ((Long) obj[0]),  // id
                        (String) obj[1],                     // schemeName
                        (String) obj[2]                      // schemeCode
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<RetailerSchemeAllocationResponse> getAllocatedSchemesForRetailer(String hierarchyId) {
        long startTime = System.currentTimeMillis();
        String loginHierarchy = tokenUtil.extractHierarchyId();

        logInfo("START", loginHierarchy, hierarchyId);

        try {
            AccessHierarchyNode loginNode = getLoginHierarchyNode(loginHierarchy);
            List<SubProducts> visibleSubProducts = getVisibleSubProducts(loginNode);

            log.info("[RETAILER ALLOCATION] visibleSubProducts={}", visibleSubProducts.size());

            List<RetailerSchemeAllocationResponse> responses =
                    buildAllocationResponses(hierarchyId, visibleSubProducts);

            logSuccess(startTime, responses.size());
            return responses;

        } catch (Exception ex) {
            logError("Failed to fetch allocation", ex);
            throw new RuntimeException("Failed to fetch allocation", ex);
        }
    }

// ==================== PRIVATE HELPER METHODS ====================

    private AccessHierarchyNode getLoginHierarchyNode(String loginHierarchy) {
        return accessHierarchyNodeRepository
                .findById(loginHierarchy)
                .orElseThrow(() -> new RuntimeException("Hierarchy not found: " + loginHierarchy));
    }

    private List<SubProducts> getVisibleSubProducts(AccessHierarchyNode loginNode) {
        String type = loginNode.getHierarchyType();

        if (hasFullAccess(type, loginNode)) {
            log.info("[RETAILER ALLOCATION] FULL ACCESS");
            return subProductsRepository.findByIsActiveTrue();
        }

        if (isWLType(type)) {
            log.info("[RETAILER ALLOCATION] WL ACCESS");
            return subProductsRepository.findAssignedWLSubProducts(loginNode.getHierarchyId());
        }

        if (isWLAUType(loginNode)) {
            String parentId = loginNode.getParentHierarchyId();
            log.info("[RETAILER ALLOCATION] WLAU → parent={}", parentId);
            return subProductsRepository.findAssignedWLSubProducts(parentId);
        }

        log.warn("[RETAILER ALLOCATION] NO ACCESS - Unrecognized hierarchy type: {}", type);
        return Collections.emptyList();
    }

    private List<RetailerSchemeAllocationResponse>
    buildAllocationResponses(
            String hierarchyId,
            List<SubProducts> subProducts
    ) {

        Map<String, String> schemeMap =

                schemeAllocationRepository

                        .findAllByHierarchyId(
                                hierarchyId
                        )

                        .stream()

                        .collect(

                                Collectors.toMap(

                                        SchemeAllocation::getSubProductCode,

                                        SchemeAllocation::getSchemeCode,

                                        (a, b) -> a
                                )
                        );

        Map<String, String> schemeNames =

                schemeRepository

                        .findBySchemeCodeInAndIsActiveTrue(

                                new ArrayList<>(
                                        schemeMap.values()
                                )

                        )

                        .stream()

                        .collect(

                                Collectors.toMap(

                                        Scheme::getSchemeCode,

                                        Scheme::getSchemeName
                                )
                        );

        return subProducts

                .stream()

                .map(sub -> {

                    String schemeName =

                            schemeNames.getOrDefault(

                                    schemeMap.get(
                                            sub.getSubProductCode()
                                    ),

                                    "No scheme assigned"
                            );

                    return new RetailerSchemeAllocationResponse(

                            sub.getSubProductTitle(),

                            sub.getSubProductCode(),

                            schemeName
                    );

                })

                .toList();
    }

    private RetailerSchemeAllocationResponse buildSingleResponse(
            String hierarchyId,
            SubProducts subProduct) {

        String schemeName = findAssignedSchemeName(hierarchyId, subProduct.getSubProductCode());

        return new RetailerSchemeAllocationResponse(
                subProduct.getSubProductTitle(),
                subProduct.getSubProductCode(),
                schemeName
        );
    }

    private String findAssignedSchemeName(String hierarchyId, String subProductCode) {
        return schemeAllocationRepository
                .findByHierarchyIdAndSubProductCode(hierarchyId, subProductCode)
                .map(allocation -> getSchemeName(allocation.getSchemeCode()))
                .orElse("No scheme assigned");
    }

    private String getSchemeName(String schemeCode) {
        return schemeRepository
                .findBySchemeCodeAndIsActiveTrue(schemeCode)
                .map(Scheme::getSchemeName)
                .orElse("No scheme assigned");
    }

// ==================== ACCESS TYPE CHECKS ====================

    private boolean hasFullAccess(String type, AccessHierarchyNode node) {
        return "AD".equalsIgnoreCase(type) || isUnderAD(node);
    }

    private boolean isUnderAD(
            AccessHierarchyNode node) {

        if (!"AU".equalsIgnoreCase(node.getHierarchyType())) {
            return false;
        }

        String parentId = node.getParentHierarchyId();

        if (parentId == null || "-".equals(parentId)) {
            return false;
        }

        return accessHierarchyNodeRepository.findById(parentId)
                .map(x -> "AD".equalsIgnoreCase(x.getHierarchyType()))
                .orElse(false);
    }

    private boolean isWLType(String type) {
        return "WL".equalsIgnoreCase(type);
    }

    private boolean isWLAUType(
            AccessHierarchyNode node
    ) {

        if (
                !"AU".equalsIgnoreCase(
                        node.getHierarchyType()
                )
        ) {
            return false;
        }

        String parent =
                node.getParentHierarchyId();

        if (
                parent == null
                        ||
                        "-".equals(parent)
        ) {
            return false;
        }

        return accessHierarchyNodeRepository

                .findById(
                        parent
                )

                .map(
                        x ->
                                "WL".equalsIgnoreCase(
                                        x.getHierarchyType()
                                )
                )

                .orElse(false);
    }

// ==================== LOGGING HELPERS ====================

    private void logInfo(String stage, String loginHierarchy, String retailerHierarchy) {
        log.info("[RETAILER ALLOCATION] {} | login={} | retailer={}",
                stage, loginHierarchy, retailerHierarchy);
    }

    private void logSuccess(long startTime, int responseCount) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("[RETAILER ALLOCATION] SUCCESS total={} time={}ms", responseCount, duration);
    }

    private void logError(String message, Exception ex) {
        log.error("[RETAILER ALLOCATION] ERROR - {}", message, ex);
    }

//    @Override
//    public Page<UserSearchResponseDto> searchActiveRetailers(
//            String mobileNumber,
//            String hierarchyId,
//            String hierarchyCode,
//            String createdDate,
//            String lastLoginDate,
//            String emailId,
//            Pageable pageable,
//            String keyword
//    ) {
//
//        long start = System.currentTimeMillis();
//
//        try {
//
//            String loginHierarchy = tokenUtil.extractHierarchyId();
//
//            log.info("[SEARCH RETAILER] START login={} keyword={}",loginHierarchy,keyword);
//
//            AccessHierarchyNode loginNode =
//                    accessHierarchyNodeRepository
//                            .findById(loginHierarchy)
//                            .orElseThrow(() ->
//                                    new RuntimeException(
//                                            "Hierarchy not found : "
//                                                    + loginHierarchy
//                                    )
//                            );
//
//            String type = loginNode.getHierarchyType();
//
//            List<String> accessibleIds = null;
//
//            /*
//             * AD
//             * full access
//             */
//            if ("AD".equalsIgnoreCase(type)) {
//
//                log.info("[SEARCH RETAILER] FULL ACCESS");
//            }
//
//            /*
//             * WLAU
//             * use parent WL
//             */
//            else if (
//                    type.endsWith("AU")
//                            &&
//                            loginNode.getParentHierarchyId() != null
//                            &&
//                            loginNode
//                                    .getParentHierarchyId()
//                                    .contains("-WL-")
//            ) {
//
//                String parent = loginNode.getParentHierarchyId();
//
//                accessibleIds =
//                        accessHierarchyPathRepository
//                                .findDescendantsByAncestor(
//                                        parent
//                                );
//
//                accessibleIds.add(parent);
//
//                log.info("[SEARCH RETAILER] WLAU ACCESS parent={} total={}",parent,accessibleIds.size());
//            }
//
//            /*
//             * ALL OTHER USERS
//             */
//            else {
//
//                accessibleIds =
//                        accessHierarchyPathRepository
//                                .findDescendantsByAncestor(
//                                        loginHierarchy
//                                );
//
//                accessibleIds.add(loginHierarchy);
//
//                log.info("[SEARCH RETAILER] ACCESS ids={}",accessibleIds.size());
//            }
//
//            if (
//                    accessibleIds != null && accessibleIds.isEmpty()
//            ) {
//
//                log.warn("[SEARCH RETAILER] NO ACCESS");
//
//                return Page.empty(pageable);
//            }
//
//            Page<UserDetails> page =
//
//                    userDetailsRepository
//                            .searchActiveRetailers(
//                                    keyword,
//                                    accessibleIds,
//                                    pageable
//                            );
//
//            List<UserSearchResponseDto> filtered =
//
//                    page.getContent()
//
//                            .stream()
//
//                            .map(u -> {
//
//                                UserSearchResponseDto dto =
//                                        new UserSearchResponseDto();
//
//                                dto.setMobileNumber(
//                                        u.getMobileNumber()
//                                );
//
//                                dto.setHierarchyId(
//                                        u.getHierarchyId()
//                                );
//
//                                dto.setHierarchyCode(
//                                        u.getHierarchyCode()
//                                );
//
//                                dto.setCreatedDate(
//                                        u.getCreatedDate()
//                                );
//
//                                dto.setLastLoginDate(
//                                        u.getLastLoginDate()
//                                );
//
//                                dto.setEmailId(
//                                        u.getEmailId()
//                                );
//
//                                return dto;
//
//                            })
//
//                            .filter(dto ->
//                                    mobileNumber == null
//                                            || mobileNumber.isBlank()
//                                            || dto.getMobileNumber()
//                                            .toLowerCase()
//                                            .contains(
//                                                    mobileNumber.toLowerCase()
//                                            )
//                            )
//
//                            .filter(dto ->
//                                    hierarchyId == null
//                                            || hierarchyId.isBlank()
//                                            || dto.getHierarchyId()
//                                            .toLowerCase()
//                                            .contains(
//                                                    hierarchyId.toLowerCase()
//                                            )
//                            )
//
//                            .filter(dto ->
//                                    hierarchyCode == null
//                                            || hierarchyCode.isBlank()
//                                            || dto.getHierarchyCode()
//                                            .toLowerCase()
//                                            .contains(
//                                                    hierarchyCode.toLowerCase()
//                                            )
//                            )
//
//                            .filter(dto ->
//                                    createdDate == null
//                                            || createdDate.isBlank()
//                                            || dto.getCreatedDate()
//                                            .toString()
//                                            .contains(
//                                                    createdDate
//                                            )
//                            )
//
//                            .filter(dto ->
//                                    lastLoginDate == null
//                                            || lastLoginDate.isBlank()
//                                            || (
//                                            dto.getLastLoginDate()
//                                                    != null
//                                                    &&
//                                                    dto.getLastLoginDate()
//                                                            .toString()
//                                                            .contains(
//                                                                    lastLoginDate
//                                                            )
//                                    )
//                            )
//
//                            .filter(dto ->
//                                    emailId == null
//                                            || emailId.isBlank()
//                                            || (
//                                            dto.getEmailId()
//                                                    != null
//                                                    &&
//                                                    dto.getEmailId()
//                                                            .toLowerCase()
//                                                            .contains(
//                                                                    emailId.toLowerCase()
//                                                            )
//                                    )
//                            )
//
//                            .toList();
//
//            log.info(
//                    "[SEARCH RETAILER] SUCCESS total={} time={}ms",
//                    filtered.size(),
//                    System.currentTimeMillis() - start
//            );
//
//            return new PageImpl<>(
//
//                    filtered,
//
//                    pageable,
//
//                    page.getTotalElements()
//            );
//
//        }
//
//        catch (Exception ex) {
//
//            log.error("[SEARCH RETAILER] ERROR",ex);
//
//            throw new RuntimeException("Failed to search retailers",ex);
//        }
//    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSearchResponseDto> searchActiveHierarchies(

            String mobileNumber,

            String hierarchyId,

            String hierarchyCode,

            String createdDate,

            String lastLoginDate,

            String emailId,

            String keyword,

            Pageable pageable
    ) {

        long startTime = System.currentTimeMillis();

        try {

            String loginHierarchyId = tokenUtil.extractHierarchyId();

            log.info(
                    "[ACTIVE HIERARCHY SEARCH] START. loginHierarchyId={}, " +
                            "keyword={}, hierarchyCode={}, page={}, size={}",
                    loginHierarchyId,
                    keyword,
                    hierarchyCode,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );

            AccessHierarchyNode loginNode =
                    accessHierarchyNodeRepository
                            .findById(loginHierarchyId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Hierarchy not found: "
                                                    + loginHierarchyId
                                    )
                            );

            String loginHierarchyType =
                    normalizeValue(loginNode.getHierarchyType());

            /*
             * Only these hierarchy types should appear
             * on the Scheme Allocation screen.
             */
            List<String> allowedHierarchyCodes =
                    List.of(
                            "WL",
                            "SD",
                            "PT",
                            "DT",
                            "RT"
                    );

            boolean fullAccess =
                    "AD".equalsIgnoreCase(loginHierarchyType);

            List<String> accessibleIds;

            if (fullAccess) {

                accessibleIds = List.of("__FULL_ACCESS__");

                log.info(
                        "[ACTIVE HIERARCHY SEARCH] Admin full access enabled."
                );

            } else if (
                    loginHierarchyType != null
                            && loginHierarchyType.endsWith("AU")
                            && loginNode.getParentHierarchyId() != null
                            && loginNode
                            .getParentHierarchyId()
                            .contains("-WL-")
            ) {

                String parentWhiteLabelId =
                        loginNode.getParentHierarchyId().trim();

                accessibleIds = new ArrayList<>(
                        accessHierarchyPathRepository
                                .findDescendantsByAncestor(
                                        parentWhiteLabelId
                                )
                );

                /*
                 * Hide the logged-in WLAU.
                 * Keep parent WL and its descendants.
                 */
                accessibleIds.removeIf(
                        id -> id.equalsIgnoreCase(loginHierarchyId)
                );

                log.info(
                        "[ACTIVE HIERARCHY SEARCH] WLAU access enabled. " +
                                "loginHierarchyId={}, parentWhiteLabelId={}, accessibleCount={}",
                        loginHierarchyId,
                        parentWhiteLabelId,
                        accessibleIds.size()
                );

            } else {

                accessibleIds = new ArrayList<>(
                        accessHierarchyPathRepository
                                .findDescendantsByAncestor(
                                        loginHierarchyId
                                )
                );

                /*
                 * Hide the currently logged-in hierarchy.
                 */
                accessibleIds.removeIf(
                        id -> id.equalsIgnoreCase(loginHierarchyId)
                );

                log.info(
                        "[ACTIVE HIERARCHY SEARCH] Restricted access enabled. " +
                                "loginHierarchyId={}, accessibleCount={}",
                        loginHierarchyId,
                        accessibleIds.size()
                );
            }

//            if (!fullAccess && accessibleIds.isEmpty()) {
//
//                log.warn(
//                        "[ACTIVE HIERARCHY SEARCH] No accessible hierarchy found. " +
//                                "loginHierarchyId={}",
//                        loginHierarchyId
//                );
//
//                return Page.empty(pageable);
//            }

            Page<UserDetails> userPage =
                    userDetailsRepository
                            .searchActiveHierarchies(
                                    allowedHierarchyCodes,
                                    fullAccess,
                                    accessibleIds,
                                    normalizeValue(keyword),
                                    normalizeValue(mobileNumber),
                                    normalizeValue(hierarchyId),
                                    normalizeValue(hierarchyCode),
                                    normalizeValue(createdDate),
                                    normalizeValue(lastLoginDate),
                                    normalizeValue(emailId),
                                    pageable
                            );

            Page<UserSearchResponseDto> responsePage =
                    userPage.map(this::mapToUserSearchResponseDto);

            log.info(
                    "[ACTIVE HIERARCHY SEARCH] SUCCESS. " +
                            "loginHierarchyId={}, currentPageRecords={}, " +
                            "totalRecords={}, totalPages={}, timeTaken={}ms",
                    loginHierarchyId,
                    responsePage.getNumberOfElements(),
                    responsePage.getTotalElements(),
                    responsePage.getTotalPages(),
                    System.currentTimeMillis() - startTime
            );

            return responsePage;

        } catch (Exception ex) {

            log.error(
                    "[ACTIVE HIERARCHY SEARCH] ERROR. message={}",
                    ex.getMessage(),
                    ex
            );

            throw new RuntimeException(
                    "Failed to search active hierarchy users: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    private void validateSchemeAllocationRequest(
            SchemeAllocationRequestDto request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Scheme allocation request cannot be null."
            );
        }

        if (request.getHierarchyId() == null
                || request.getHierarchyId().isBlank()) {

            throw new IllegalArgumentException(
                    "Hierarchy ID is required."
            );
        }

        if (request.getCodes() == null
                || request.getCodes().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one subproduct and scheme must be provided."
            );
        }
    }

    private String normalizeRequiredValue(
            String value,
            String errorMessage
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }

    private String normalizeWhiteLabelId(String whiteLabelId) {

        if (whiteLabelId == null) {
            return null;
        }

        String normalized = whiteLabelId.trim();

        if (normalized.isEmpty()
                || "-".equals(normalized)
                || "null".equalsIgnoreCase(normalized)) {

            return null;
        }

        return normalized;
    }


    private void validateHierarchyCanReceiveSubProduct(
            String targetHierarchyId,
            SubProducts subProduct
    ) {

        String subProductCode = subProduct.getSubProductCode();

        /*
         * When the subproduct is White Label controlled,
         * it must have a valid assigned White Label.
         */
        if (!Boolean.TRUE.equals(subProduct.getIsWhiteLabelAdmin())) {

            log.debug(
                    "Subproduct is not restricted by White Label. " +
                            "Skipping hierarchy validation. subProductCode={}",
                    subProductCode
            );

            return;
        }

        String assignedWhiteLabelId =
                normalizeWhiteLabelId(subProduct.getWhiteLabelAdminId());

        if (assignedWhiteLabelId == null) {

            log.error(
                    "Subproduct has White Label restriction enabled, " +
                            "but White Label ID is missing. subProductCode={}",
                    subProductCode
            );

            throw new IllegalStateException(
                    "Subproduct " + subProductCode
                            + " has no valid White Label assigned."
            );
        }

        /*
         * Verify that:
         *
         * assignedWhiteLabelId is ancestor
         * targetHierarchyId is descendant
         */
        long hierarchyPathCount =
                accessHierarchyPathRepository
                        .countHierarchyUnderWhiteLabel(
                                assignedWhiteLabelId,
                                targetHierarchyId
                        );

        if (hierarchyPathCount <= 0) {

            log.warn(
                    "Scheme allocation rejected. Target hierarchy does not " +
                            "belong to the White Label assigned to the subproduct. " +
                            "hierarchyId={}, subProductCode={}, assignedWhiteLabelId={}",
                    targetHierarchyId,
                    subProductCode,
                    assignedWhiteLabelId
            );

            throw new IllegalArgumentException(
                    "Cannot assign subproduct "
                            + subProductCode
                            + " to hierarchy "
                            + targetHierarchyId
                            + ". This subproduct belongs to White Label "
                            + assignedWhiteLabelId
                            + ", but the selected hierarchy is not under that White Label."
            );
        }

        log.info(
                "White Label hierarchy validation successful. " +
                        "hierarchyId={}, subProductCode={}, assignedWhiteLabelId={}",
                targetHierarchyId,
                subProductCode,
                assignedWhiteLabelId
        );
    }

    private String normalizeValue(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private UserSearchResponseDto mapToUserSearchResponseDto(
            UserDetails user
    ) {

        UserSearchResponseDto dto =
                new UserSearchResponseDto();

        dto.setMobileNumber(
                user.getMobileNumber()
        );

        dto.setHierarchyId(
                user.getHierarchyId()
        );

        dto.setHierarchyCode(
                user.getHierarchyCode()
        );

        dto.setCreatedDate(
                user.getCreatedDate()
        );

        dto.setLastLoginDate(
                user.getLastLoginDate()
        );

        dto.setEmailId(
                user.getEmailId()
        );

        return dto;
    }


}
