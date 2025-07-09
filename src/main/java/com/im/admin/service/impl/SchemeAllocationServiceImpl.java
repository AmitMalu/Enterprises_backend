package com.im.admin.service.impl;


import com.im.admin.dto.SchemeAllocationDto;
import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.entities.SubProducts;
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

import java.math.BigInteger;
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
    public CommonResponse<List<SchemeAllocation>> allocateSchemes(SchemeAllocationRequestDto request) {
        List<SchemeAllocation> schemeEntities = new ArrayList<>();

        for (SchemeAllocationDto scheme : request.getCodes()) {
            boolean exists = schemeAllocationRepository.existsByHierarchyIdAndProductCodeAndSubProductCode(
                    request.getHierarchyId(),
                    scheme.getProductCode(),
                    scheme.getSubProductCode()
//                    scheme.getOperatorCode()
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
            //entity.setOperatorCode(scheme.getOperatorCode());

            schemeEntities.add(entity);
        }

        schemeAllocationRepository.saveAll(schemeEntities);

        return new CommonResponse<>(
                200,
                "Schemes allocated successfully for " + request.getHierarchyId(),
                schemeEntities
        );
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
