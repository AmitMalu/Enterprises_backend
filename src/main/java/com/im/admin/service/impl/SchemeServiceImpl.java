package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.exception.ResourceNotFoundException;
import com.im.admin.mapper.SchemeAccessResolver;
import com.im.admin.mapper.SchemeMapper;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GetAllSchemeResponse;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.SchemeService;
import com.im.admin.spec.SchemeSpecification;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class SchemeServiceImpl implements SchemeService {

    @Autowired
    private SchemeHierarchySelectionRepository schemeHierarchySelectionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private SchemeAllocationRepository repository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

    @Autowired
    private HierarchyAccessService hierarchyAccessService;

    @Autowired
    private SchemeMapper schemeMapper;

    @Autowired
    private SchemeAccessResolver schemeAccessResolver;

    public List<Map<String, Object>> getAllSchemeHierarchySelections() {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findAll();

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getCombinationHashCode());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchSchemeHierarchy(String keyword) {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findBySchemeHierarchyCombinationContainingIgnoreCase(keyword);

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getSchemeHierarchyCombination());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> saveScheme(SchemeDTO schemeDTO) {
        log.info("Saving new scheme: {}", schemeDTO);

        // Directly use request values to form combination hash
        String schemeCombinationHash = schemeDTO.getProductId() + "-" +
                schemeDTO.getSubProductId() + "-" +
                //schemeDTO.getOperatorId() +"-"+
                schemeDTO.getVendorId() + "-" +
                schemeDTO.getSchemeHierarchySelectionId();

        // Check for duplicate Scheme Name
        if (schemeRepository.existsBySchemeName(schemeDTO.getSchemeName())) {
            throw new DuplicateEntryException("Scheme Name already exists!");
        }

        // Check for duplicate Scheme Code
        if (schemeRepository.existsBySchemeCode(schemeDTO.getSchemeCode())) {
            throw new DuplicateEntryException("Scheme Code already exists!");
        }


        Long maxId = schemeRepository.findMaxSchemeId();
        if (maxId == null) maxId = 0L;

        if (maxId >= 9999) {
            throw new IllegalStateException("Scheme ID limit exceeded (0001-99999). Cannot save new scheme.");
        }

        String formattedSchemeId = String.format("%05d", maxId + 1);
        String finalCombinationHashCode = schemeCombinationHash + "-" + formattedSchemeId;

        Scheme scheme = new Scheme();
        scheme.setSchemeName(schemeDTO.getSchemeName());
//        scheme.setSchemeCode(schemeDTO.getSchemeCode());
        scheme.setSchemeCombinationHashCode(finalCombinationHashCode);
        scheme.setIsActive(schemeDTO.getIsActive());
        scheme.setCreatedBy(schemeDTO.getCreatedBy());

        Scheme save = schemeRepository.save(scheme);
        Long generatedId = save.getId();

        scheme.setSchemeCode(String.valueOf(generatedId));
        save = schemeRepository.save(scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Scheme saved successfully.");
        response.put("schemeId", formattedSchemeId);
        response.put("schemeCombinationHashCode", finalCombinationHashCode);

        log.info("Scheme saved successfully: {}", response);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> updateScheme(String schemeId, SchemeDTO schemeDTO) {
        log.info("Updating scheme ID {}: {}", schemeId, schemeDTO);

        Scheme scheme = schemeRepository.findById(Long.valueOf(schemeId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + schemeId));

        // Check for duplicate scheme name if changed
        if (!scheme.getSchemeName().equals(schemeDTO.getSchemeName()) &&
                schemeRepository.existsBySchemeName(schemeDTO.getSchemeName())) {
            throw new DuplicateEntryException("Scheme Name already exists!");
        }

        // Check for duplicate scheme code if changed
        if (!scheme.getSchemeCode().equals(schemeDTO.getSchemeCode()) &&
                schemeRepository.existsBySchemeCode(schemeDTO.getSchemeCode())) {
            throw new DuplicateEntryException("Scheme Code already exists!");
        }

        // Extract last 4-digit scheme ID part from existing hash (e.g., "0009")
        String oldHashCode = scheme.getSchemeCombinationHashCode();
        String last4Digits = oldHashCode != null && oldHashCode.contains("-")
                ? oldHashCode.substring(oldHashCode.lastIndexOf("-") + 1)
                : String.format("%04d", scheme.getId()); // fallback if not set

        // Rebuild the hash using the new values, but keep the original suffix
        String schemeCombinationHash = schemeDTO.getProductId() + "-" +
                schemeDTO.getSubProductId() + "-" +
                //schemeDTO.getOperatorId() + "-" +
                schemeDTO.getVendorId() + "-" +
                schemeDTO.getSchemeHierarchySelectionId();

        String newCombinationHashCode = schemeCombinationHash + "-" + last4Digits;

        scheme.setSchemeName(schemeDTO.getSchemeName());
//        scheme.setSchemeCode(schemeDTO.getSchemeCode());
        scheme.setSchemeCombinationHashCode(newCombinationHashCode);
        scheme.setEditedBy("Admin");
        scheme.setEditedOn(LocalDateTime.now());
        scheme.setIsActive(schemeDTO.getIsActive());

        schemeRepository.save(scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Scheme updated successfully.");
        response.put("schemeId", last4Digits);
        response.put("schemeCombinationHashCode", newCombinationHashCode);

        log.info("Scheme updated successfully: {}", response);
        return response;
    }

    public OperatorProductResponseDTO getOperatorsAndProduct(String productCode, String subProductCode) {
        List<Operators> operators = operatorsRepository.findBySubProductCode(subProductCode);

        if (operators.isEmpty()) {
            throw new RuntimeException("No operators found for subProductCode: " + subProductCode);
        }

        List<OperatorDropdownResponseDTO> operatorDTOs = operators.stream()
                .map(op -> new OperatorDropdownResponseDTO(op.getId(), op.getOperatorName(), op.getOperatorCode()))
                .collect(Collectors.toList());

        Products product = productsRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found for productCode: " + productCode));

        return new OperatorProductResponseDTO(operatorDTOs, product);
    }

    @Override
    public List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode) {
        return schemeRateRepository.findByOperatorCode(operatorCode);
    }

    @Override
    public CommonResponse<Page<GetAllSchemeResponse>>
    getAllSchemes(

            SchemeFilter filter,

            Pageable pageable

    ) {

        long start =
                System.currentTimeMillis();

        try {

            //there is mismatch while returning a response
            //1.if NFT-AD-001 create a subproduct and not selected a this below means this subproduct is not belongs to this white label right ?
            //2.and NFT-AD-001 create a scheme so in out that time while creating scheme we select subproduct and other things so schemeCombinationhashCOde is created
            //as per that we return a response a for white label although AD can see all scheme

            // @Column(name = "is_white_label_admin")
            //    private Boolean isWhiteLabelAdmin;
            //
            //    @Column(name = "white_label_admin_id")
            //    private String whiteLabelAdminId;

            // but issue is while creating subproduct we not selected a white label even though it apperans in scheme list while WL is login
            //and as you know our AU and WLAU logic

            log.info(
                    "[SCHEME] START filter={}",
                    filter
            );

            String loginId =
                    tokenUtil
                            .extractHierarchyId();

            log.info(
                    "[SCHEME] Login={}",
                    loginId
            );

            SchemeAccessDto access =
                    schemeAccessResolver
                            .resolve(
                                    loginId
                            );

            Specification<Scheme> spec =

                    SchemeSpecification
                            .build(

                                    filter,

                                    access
                                            .getAllowedSubProducts(),

                                    access
                                            .isFullAccess()

                            );

            Page<GetAllSchemeResponse>
                    result =

                    schemeRepository

                            .findAll(
                                    spec,
                                    pageable
                            )

                            .map(
                                    schemeMapper
                                            ::toResponse
                            );

            log.info(
                    "[SCHEME] SUCCESS total={}",
                    result
                            .getTotalElements()
            );

            return CommonResponse.success(

                    result,

                    "Schemes fetched successfully"
            );

        }

        catch (

                Exception ex

        ) {

            log.error(
                    "[SCHEME] ERROR",
                    ex
            );

            return CommonResponse.failedResponse(

                    500,

                    "Failed to fetch schemes",

                    Page.empty()

            );

        }

    }



    @Override
    public GetAllSchemeResponse getSchemeDtoById(Long id) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scheme not found with ID: " + id));

        String[] parts = scheme.getSchemeCombinationHashCode().split("-");

        String productId = parts.length > 0 ? parts[0] : null;
        String subProductId = parts.length > 1 ? parts[1] : null;
        String vendorId = parts.length > 2 ? parts[2] : null;
        String schemeHierarchyId = parts.length > 3 ? parts[3] : null;

        String productName = null;
        if (productId != null) {
            productName = productsRepository.findById(Short.valueOf(productId))
                    .map(Products::getName)
                    .orElse("N/A");
        }

        String subProductName = null;
        if (subProductId != null) {
            subProductName = subProductsRepository.findById(subProductId)
                    .map(SubProducts::getSubProductTitle)
                    .orElse("N/A");
        }

        String vendorName = null;
        if (vendorId != null) {
            vendorName = vendorRepository.findById(vendorId)
                    .map(Vendors::getVendorName)
                    .orElse("N/A");
        }

        String schemeHierarchyName = null;
        if (schemeHierarchyId != null) {
            schemeHierarchyName = schemeHierarchySelectionRepository.findByCustomId(schemeHierarchyId)
                    .map(SchemeHierarchySelection::getSchemeHierarchyCombination).orElse("N/A");
        }

        return new GetAllSchemeResponse(
                scheme.getId(),
                scheme.getSchemeName(),
                scheme.getSchemeCode(),
                scheme.getSchemeCombinationHashCode(),
                productId,
                productName,
                subProductId,
                subProductName,
                vendorId,
                vendorName,
                schemeHierarchyName,
                schemeHierarchyId,
                scheme.getCreatedOn(),
                scheme.getCreatedBy(),
                scheme.getEditedOn(),
                scheme.getEditedBy(),
                scheme.getIsActive()
        );
    }

    @Override
    @Transactional
    public void updateSchemeStatus(String schemeCode, boolean isActive) {
        Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> new RuntimeException("Scheme not found for scheme code: " + schemeCode));

        scheme.setIsActive(isActive);
        schemeRepository.save(scheme);
    }

}
