package com.im.admin.service.impl;


import com.im.admin.dto.SchemeAllocationDto;
import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.entities.SubProducts;
import com.im.admin.exception.ResourceNotFoundException;
import com.im.admin.repositories.SchemeAllocationRepository;
import com.im.admin.repositories.SchemeRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;
import com.im.admin.service.SchemeAllocationService;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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


    @Override
    @Transactional
    public List<SchemeAllocation> assignOrUpdateSchemes(SchemeAllocationRequestDto request) {
        List<SchemeAllocation> result = new ArrayList<>();

        for (SchemeAllocationDto dto : request.getCodes()) {
            // 1. Find product by subproduct
            SubProducts subProduct = subProductsRepository.findBySubProductCode(dto.getSubProductCode())
                    .orElseThrow(() -> new ResourceNotFoundException("SubProduct not found: " + dto.getSubProductCode()));

            String productCode = subProduct.getProductCode();

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

        List<SubProducts> activeSubProducts = subProductsRepository.findByIsActiveTrue();
        List<RetailerSchemeAllocationResponse> responseList = new ArrayList<>();

        for (SubProducts subProduct : activeSubProducts) {
            String subProductCode = subProduct.getSubProductCode();
            String subProductName = subProduct.getSubProductTitle();

            String schemeName = "No scheme assigned";

            Optional<SchemeAllocation> allocation = schemeAllocationRepository
                    .findByHierarchyIdAndSubProductCode(hierarchyId, subProductCode);

            if (allocation.isPresent()) {
                String schemeCode = allocation.get().getSchemeCode();

                Optional<Scheme> scheme = schemeRepository.findBySchemeCodeAndIsActiveTrue(schemeCode);
                schemeName = scheme.map(Scheme::getSchemeName).orElse("No scheme assigned");
            }

            responseList.add(new RetailerSchemeAllocationResponse(subProductName, schemeName));
        }

        return responseList;
    }


}
