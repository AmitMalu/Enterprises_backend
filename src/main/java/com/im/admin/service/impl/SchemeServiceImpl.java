package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.product.Products;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GetAllSchemeResponse;
import com.im.admin.service.SchemeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    private SchemeAllocationRepository repository;

    public List<Map<String, Object>> getAllSchemeHierarchySelections() {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findAll();

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getCombinationHash());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchSchemeHierarchy(String keyword) {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findByCombinationHashContainingIgnoreCase(keyword);

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getCombinationHash());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> saveScheme(SchemeDTO schemeDTO) {
        log.info("Saving new scheme: {}", schemeDTO);

        // Directly use request values to form combination hash
        String schemeCombinationHash = schemeDTO.getProductId() +"-"+
                schemeDTO.getSubProductId() +"-"+
                schemeDTO.getOperatorId() +"-"+
                schemeDTO.getVendorId() +"-"+
                schemeDTO.getSchemeHierarchySelectionId();

        // Check for duplicate Scheme Name
        if (schemeRepository.existsBySchemeName(schemeDTO.getSchemeName())) {
            throw new DuplicateEntryException("Scheme Name already exists!");
        }

        // Check for duplicate Scheme Code
        if (schemeRepository.existsBySchemeCode(schemeDTO.getSchemeCode())) {
            throw new DuplicateEntryException("Scheme Code already exists!");
        }

        // Check if a scheme already exists with the same prefix
//        if (schemeRepository.existsBySchemeCombinationHashCodeStartingWith(schemeCombinationHash)) {
//            throw new DuplicateEntryException("This scheme combination already exists!");
//        }


        Long maxId = schemeRepository.findMaxSchemeId();
        if (maxId == null) maxId = 0L;

        if (maxId >= 9999) {
            throw new IllegalStateException("Scheme ID limit exceeded (0001-9999). Cannot save new scheme.");
        }

        String formattedSchemeId = String.format("%04d", maxId + 1);
        String finalCombinationHashCode = schemeCombinationHash +"-"+ formattedSchemeId;

        Scheme scheme = new Scheme();
        scheme.setSchemeName(schemeDTO.getSchemeName());
        scheme.setSchemeCode(schemeDTO.getSchemeCode());
        scheme.setSchemeCombinationHashCode(finalCombinationHashCode);
        scheme.setCreatedBy("Admin");

        schemeRepository.save(scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Scheme saved successfully.");
        response.put("schemeId", formattedSchemeId);
        response.put("schemeCombinationHashCode", finalCombinationHashCode);

        log.info("Scheme saved successfully: {}", response);
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
    public List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO) {
        List<SchemeRate> schemeRates = new ArrayList<>();

        for (int i = 0; i < requestDTO.getSlabs().size(); i++) {
            SchemeRateSlabDTO slab = requestDTO.getSlabs().get(i);
            SchemeRate schemeRate = new SchemeRate();

            schemeRate.setSchemeCode(requestDTO.getSchemeCode());
            schemeRate.setOperatorName(requestDTO.getOperatorName());
            schemeRate.setOperatorCode(requestDTO.getOperatorCode());
            schemeRate.setRateType(slab.getRateType());
            schemeRate.setSlabStart(slab.getSlabStart());
            schemeRate.setSlabEnd(slab.getSlabEnd());

            // Ensure gross concept: sum of all fields must be exactly equal to gross
            double im = slab.getIm() == null ? 0.0 : slab.getIm();
            double enterprise = slab.getEnterprise() == null ? 0.0 : slab.getEnterprise();
            double whitelabel = slab.getWhitelabel() == null ? 0.0 : slab.getWhitelabel();
            double superDistributor = slab.getSuperDistributor() == null ? 0.0 : slab.getSuperDistributor();
            double partner = slab.getPartner() == null ? 0.0 : slab.getPartner();
            double distributor = slab.getDistributor() == null ? 0.0 : slab.getDistributor();
            double retailer = slab.getRetailer() == null ? 0.0 : slab.getRetailer();

            double totalDistributed = im + enterprise + whitelabel + superDistributor + partner + distributor + retailer;
            double gross = slab.getGross();

            if (!Objects.equals(gross, totalDistributed)) {
                throw new IllegalArgumentException("Error in slab #" + (i + 1) + ": Sum of all fields must be exactly equal to Gross (Expected: " + gross + ", Found: " + totalDistributed + ").");
            }

            // Store values based on rateType
            if (slab.getRateType() == RateType.PERCENTAGE) {
                schemeRate.setGross(gross / 100);
                schemeRate.setIm(im / 100);
                schemeRate.setEnterprise(enterprise / 100);
                schemeRate.setWhitelabel(whitelabel / 100);
                schemeRate.setSuperDistributor(superDistributor / 100);
                schemeRate.setPartner(partner / 100);
                schemeRate.setDistributor(distributor / 100);
                schemeRate.setRetailer(retailer / 100);
            } else {
                schemeRate.setGross(gross);
                schemeRate.setIm(im);
                schemeRate.setEnterprise(enterprise);
                schemeRate.setWhitelabel(whitelabel);
                schemeRate.setSuperDistributor(superDistributor);
                schemeRate.setPartner(partner);
                schemeRate.setDistributor(distributor);
                schemeRate.setRetailer(retailer);
            }

            schemeRate.setCreatedBy("admin");
            schemeRates.add(schemeRate);
        }

        return schemeRateRepository.saveAll(schemeRates);
    }

    @Override
    public List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode) {
        return schemeRateRepository.findByOperatorCode(operatorCode);
    }

    @Override
    public CommonResponse<List<SchemeAllocation>> allocateSchemes(SchemeAllocationRequestDto request) {
        List<SchemeAllocation> schemeEntities = new ArrayList<>();

        for (SchemeAllocationDto scheme : request.getCodes()) {
            boolean exists = repository.existsByHierarchyIdAndProductCodeAndSubProductCodeAndOperatorCode(
                    request.getHierarchyId(),
                    scheme.getProductCode(),
                    scheme.getSubProductCode(),
                    scheme.getOperatorCode()
            );

            if (exists) {
                return new CommonResponse<>(
                        400,
                        "Scheme already allocated for hierarchy: " + request.getHierarchyId() +
                                ", product: " + scheme.getProductCode() +
                                ", subProduct: " + scheme.getSubProductCode() +
                                ", operator: " + scheme.getOperatorCode(),
                        null
                );
            }

            SchemeAllocation entity = new SchemeAllocation();
            entity.setHierarchyId(request.getHierarchyId());
            entity.setProductCode(scheme.getProductCode());
            entity.setSubProductCode(scheme.getSubProductCode());
            entity.setSchemeCode(scheme.getSchemeCode());
            entity.setOperatorCode(scheme.getOperatorCode());
            entity.setSchemeId(scheme.getSchemeId());

            schemeEntities.add(entity);
        }

        repository.saveAll(schemeEntities);

        return new CommonResponse<>(
                200,
                "Schemes allocated successfully for " + request.getHierarchyId(),
                schemeEntities
        );
    }

    @Transactional
    @Override
    public VendorRouting saveVendorRouting(VendorRoutingDto request) {
        VendorRouting vendorRouting = new VendorRouting();
        vendorRouting.setSubProductId(request.getSubProductId());
        vendorRouting.setSubProductName(request.getSubProductName());
        vendorRouting.setVendorId(request.getVendorId());
        vendorRouting.setVendorName(request.getVendorName());
        vendorRouting.setPriority(request.getPriority());
        vendorRouting.setCreatedBy(request.getCreatedBy());
        vendorRouting.setEditedBy(request.getEditedBy());

        return vendorRoutingRepository.save(vendorRouting);
    }

    @Override
    public Page<GetAllSchemeResponse> getAllSchemes(Pageable pageable) {
        Page<Scheme> schemes = schemeRepository.findAll(pageable);

        return schemes.map(scheme -> {
            String[] parts = scheme.getSchemeCombinationHashCode().split("-");

            String productId = parts.length > 0 ? parts[0] : null;
            String subProductId = parts.length > 1 ? parts[1] : null;

            String productName = productsRepository.findById(Short.valueOf(productId))
                    .map(Products::getName).orElse("N/A");

            String subProductName = subProductsRepository.findById(subProductId)
                    .map(SubProducts::getSubProductTitle).orElse("N/A");

            return new GetAllSchemeResponse(
                    scheme.getId(),
                    scheme.getSchemeName(),
                    scheme.getSchemeCode(),
                    scheme.getSchemeCombinationHashCode(),
                    productId,
                    productName,
                    subProductId,
                    subProductName,
                    scheme.getCreatedOn(),
                    scheme.getCreatedBy(),
                    scheme.getEditedOn(),
                    scheme.getEditedBy()
            );
        });
    }

    @Override
    public GetAllSchemeResponse getSchemeDtoById(Long id) {
        Scheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scheme not found with ID: " + id));

        String[] parts = scheme.getSchemeCombinationHashCode().split("-");

        String productId = parts.length > 0 ? parts[0] : null;
        String subProductId = parts.length > 1 ? parts[1] : null;

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

        return new GetAllSchemeResponse(
                scheme.getId(),
                scheme.getSchemeName(),
                scheme.getSchemeCode(),
                scheme.getSchemeCombinationHashCode(),
                productId,
                productName,
                subProductId,
                subProductName,
                scheme.getCreatedOn(),
                scheme.getCreatedBy(),
                scheme.getEditedOn(),
                scheme.getEditedBy()
        );
    }

}
