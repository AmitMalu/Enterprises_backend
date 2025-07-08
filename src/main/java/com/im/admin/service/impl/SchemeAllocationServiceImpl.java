package com.im.admin.service.impl;


import com.im.admin.dto.SchemeAllocationDto;
import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.repositories.SchemeAllocationRepository;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SchemeAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SchemeAllocationServiceImpl implements SchemeAllocationService {

    @Autowired
    private SchemeAllocationRepository repository;


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

            schemeEntities.add(entity);
        }

        repository.saveAll(schemeEntities);

        return new CommonResponse<>(
                200,
                "Schemes allocated successfully for " + request.getHierarchyId(),
                schemeEntities
        );
    }


    //make api that in the request hierarchy id, scheme code , product code , sub product code will come and save in the db also
    //check existing by this or not

    @Override
    public CommonResponse<String> saveSchemeAllocation(SchemeAllocationRequest request) {

        boolean exists = repository
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

        repository.save(allocation);

        return CommonResponse.success(null, "Scheme allocation saved successfully.");
    }
}
