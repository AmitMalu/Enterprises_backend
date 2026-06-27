package com.im.admin.service.impl;


import com.im.admin.dto.SchemeAllocationDto;
import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.*;
import com.im.admin.exception.ResourceNotFoundException;
import com.im.admin.repositories.*;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.SchemeAllocationService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
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

    @Override
    @Transactional
    public List<SchemeAllocation> assignOrUpdateSchemes(SchemeAllocationRequestDto request) {
        List<SchemeAllocation> result = new ArrayList<>();

        for (SchemeAllocationDto dto : request.getCodes()) {
            // 1. Find product by subproduct
            SubProducts subProduct = subProductsRepository.findBySubProductCode(dto.getSubProductCode())
                    .orElseThrow(() -> new ResourceNotFoundException("SubProduct not found: " + dto.getSubProductCode()));

            String productCode = subProduct.getProduct().getProductCode();

            // 2. Check if record already exists
            Optional<SchemeAllocation> existing = schemeAllocationRepository
                    .findByHierarchyIdAndProductCodeAndSubProductCode(
                            request.getHierarchyId(), productCode, dto.getSubProductCode()
                    );

            SchemeAllocation allocation;
            if (existing.isPresent()) {
                // Update
                allocation = existing.get();
                allocation.setSchemeCode(dto.getSchemeCode());
            } else {
                // Insert
                allocation = new SchemeAllocation();
                allocation.setHierarchyId(request.getHierarchyId());
                allocation.setProductCode(productCode);
                allocation.setSubProductCode(dto.getSubProductCode());
                allocation.setSchemeCode(dto.getSchemeCode());
            }

            result.add(allocation);
        }

        // Save all at once (JPA handles insert/update automatically)
        return schemeAllocationRepository.saveAll(result);
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

    @Override
    public Page<UserSearchResponseDto> searchActiveRetailers(
            String mobileNumber,
            String hierarchyId,
            String hierarchyCode,
            String createdDate,
            String lastLoginDate,
            String emailId,
            Pageable pageable,
            String keyword
    ) {

        long start = System.currentTimeMillis();

        try {

            String loginHierarchy = tokenUtil.extractHierarchyId();

            log.info("[SEARCH RETAILER] START login={} keyword={}",loginHierarchy,keyword);

            AccessHierarchyNode loginNode =
                    accessHierarchyNodeRepository
                            .findById(loginHierarchy)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Hierarchy not found : "
                                                    + loginHierarchy
                                    )
                            );

            String type = loginNode.getHierarchyType();

            List<String> accessibleIds = null;

            /*
             * AD
             * full access
             */
            if ("AD".equalsIgnoreCase(type)) {

                log.info("[SEARCH RETAILER] FULL ACCESS");
            }

            /*
             * WLAU
             * use parent WL
             */
            else if (
                    type.endsWith("AU")
                            &&
                            loginNode.getParentHierarchyId() != null
                            &&
                            loginNode
                                    .getParentHierarchyId()
                                    .contains("-WL-")
            ) {

                String parent = loginNode.getParentHierarchyId();

                accessibleIds =
                        accessHierarchyPathRepository
                                .findDescendantsByAncestor(
                                        parent
                                );

                accessibleIds.add(parent);

                log.info("[SEARCH RETAILER] WLAU ACCESS parent={} total={}",parent,accessibleIds.size());
            }

            /*
             * ALL OTHER USERS
             */
            else {

                accessibleIds =
                        accessHierarchyPathRepository
                                .findDescendantsByAncestor(
                                        loginHierarchy
                                );

                accessibleIds.add(loginHierarchy);

                log.info("[SEARCH RETAILER] ACCESS ids={}",accessibleIds.size());
            }

            if (
                    accessibleIds != null && accessibleIds.isEmpty()
            ) {

                log.warn("[SEARCH RETAILER] NO ACCESS");

                return Page.empty(pageable);
            }

            Page<UserDetails> page =

                    userDetailsRepository
                            .searchActiveRetailers(
                                    keyword,
                                    accessibleIds,
                                    pageable
                            );

            List<UserSearchResponseDto> filtered =

                    page.getContent()

                            .stream()

                            .map(u -> {

                                UserSearchResponseDto dto =
                                        new UserSearchResponseDto();

                                dto.setMobileNumber(
                                        u.getMobileNumber()
                                );

                                dto.setHierarchyId(
                                        u.getHierarchyId()
                                );

                                dto.setHierarchyCode(
                                        u.getHierarchyCode()
                                );

                                dto.setCreatedDate(
                                        u.getCreatedDate()
                                );

                                dto.setLastLoginDate(
                                        u.getLastLoginDate()
                                );

                                dto.setEmailId(
                                        u.getEmailId()
                                );

                                return dto;

                            })

                            .filter(dto ->
                                    mobileNumber == null
                                            || mobileNumber.isBlank()
                                            || dto.getMobileNumber()
                                            .toLowerCase()
                                            .contains(
                                                    mobileNumber.toLowerCase()
                                            )
                            )

                            .filter(dto ->
                                    hierarchyId == null
                                            || hierarchyId.isBlank()
                                            || dto.getHierarchyId()
                                            .toLowerCase()
                                            .contains(
                                                    hierarchyId.toLowerCase()
                                            )
                            )

                            .filter(dto ->
                                    hierarchyCode == null
                                            || hierarchyCode.isBlank()
                                            || dto.getHierarchyCode()
                                            .toLowerCase()
                                            .contains(
                                                    hierarchyCode.toLowerCase()
                                            )
                            )

                            .filter(dto ->
                                    createdDate == null
                                            || createdDate.isBlank()
                                            || dto.getCreatedDate()
                                            .toString()
                                            .contains(
                                                    createdDate
                                            )
                            )

                            .filter(dto ->
                                    lastLoginDate == null
                                            || lastLoginDate.isBlank()
                                            || (
                                            dto.getLastLoginDate()
                                                    != null
                                                    &&
                                                    dto.getLastLoginDate()
                                                            .toString()
                                                            .contains(
                                                                    lastLoginDate
                                                            )
                                    )
                            )

                            .filter(dto ->
                                    emailId == null
                                            || emailId.isBlank()
                                            || (
                                            dto.getEmailId()
                                                    != null
                                                    &&
                                                    dto.getEmailId()
                                                            .toLowerCase()
                                                            .contains(
                                                                    emailId.toLowerCase()
                                                            )
                                    )
                            )

                            .toList();

            log.info(
                    "[SEARCH RETAILER] SUCCESS total={} time={}ms",
                    filtered.size(),
                    System.currentTimeMillis() - start
            );

            return new PageImpl<>(

                    filtered,

                    pageable,

                    page.getTotalElements()
            );

        }

        catch (Exception ex) {

            log.error("[SEARCH RETAILER] ERROR",ex);

            throw new RuntimeException("Failed to search retailers",ex);
        }
    }


}
