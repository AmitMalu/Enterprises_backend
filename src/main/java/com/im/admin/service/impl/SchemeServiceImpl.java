package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.repositories.*;
import com.im.admin.service.SchemeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

        List<OperatorResponseDTO> operatorDTOs = operators.stream()
                .map(op -> new OperatorResponseDTO(op.getOperatorName(), op.getOperatorCode()))
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
            schemeRate.setSlabType(slab.getSlabType());

            if ("FIXED".equalsIgnoreCase(String.valueOf(slab.getSlabType()))) {
                if (slab.getFixedSlabValue() == null) {
                    throw new IllegalArgumentException("Error in slab #" + (i + 1) + ": Fixed slab value is required for FIXED slab type.");
                }
                schemeRate.setFixedSlabValue(slab.getFixedSlabValue());
            } else if ("VARIABLE".equalsIgnoreCase(String.valueOf(slab.getSlabType()))) {
                if (slab.getSlabStart() == null || slab.getSlabEnd() == null) {
                    throw new IllegalArgumentException("Error in slab #" + (i + 1) + ": Slab start and slab end are required for VARIABLE slab type.");
                }
                schemeRate.setSlabStart(slab.getSlabStart());
                schemeRate.setSlabEnd(slab.getSlabEnd());
            }

            Double totalDistributed = slab.getIm() + slab.getEnterprise() + slab.getWhitelabel()
                    + slab.getSuperDistributor() + slab.getPartner() + slab.getDistributor() + slab.getRetailer();

            if (!Objects.equals(slab.getGross(), totalDistributed)) {
                throw new IllegalArgumentException("Error in slab #" + (i + 1) + ": Sum of all fields must be exactly equal to Gross (Expected: " + slab.getGross() + ", Found: " + totalDistributed + ").");
            }

            schemeRate.setGross(slab.getGross());
            schemeRate.setIm(slab.getIm());
            schemeRate.setEnterprise(slab.getEnterprise());
            schemeRate.setWhitelabel(slab.getWhitelabel());
            schemeRate.setSuperDistributor(slab.getSuperDistributor());
            schemeRate.setPartner(slab.getPartner());
            schemeRate.setDistributor(slab.getDistributor());
            schemeRate.setRetailer(slab.getRetailer());

            schemeRates.add(schemeRate);
        }

        return schemeRateRepository.saveAll(schemeRates);
    }

    @Override
    public List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode) {
        return schemeRateRepository.findByOperatorCode(operatorCode);
    }

    @Override
    public void allocateSchemes(SchemeAllocationRequestDto request) {
        List<SchemeAllocation> schemeEntities = request.getCodes().stream().map(scheme -> {
            SchemeAllocation entity = new SchemeAllocation();
            entity.setHierarchyId(request.getHierarchyId());
            entity.setSubProductCode(scheme.getSubProductCode());
            entity.setSchemeCode(scheme.getSchemeCode());
            return entity;
        }).collect(Collectors.toList());

        repository.saveAll(schemeEntities);
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
}
