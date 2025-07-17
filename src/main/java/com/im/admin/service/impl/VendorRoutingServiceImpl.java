package com.im.admin.service.impl;

import com.im.admin.dto.VendorRoutingRequestDTO;
import com.im.admin.dto.VendorRoutingResponseDTO;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorRoutingService;
import com.im.admin.service.VendorService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VendorRoutingServiceImpl implements VendorRoutingService {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorService vendorService; // to fetch vendor name

    @Autowired
    private VendorRepository vendorsRepository;

    @Override
    public CommonResponse<VendorRoutingResponseDTO> save(VendorRoutingRequestDTO request) {
        VendorRouting entity = mapToEntity(request);
        VendorRouting saved = vendorRoutingRepository.save(entity);
        return CommonResponse.success(mapToDTO(saved), "Saved successfully");
    }

    @Override
    public CommonResponse<VendorRoutingResponseDTO> update(Long id, VendorRoutingRequestDTO request) {
        VendorRouting existing = vendorRoutingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VendorRouting not found"));
        updateEntity(existing, request);
        VendorRouting updated = vendorRoutingRepository.save(existing);
        return CommonResponse.success(mapToDTO(updated), "Updated successfully");
    }

    @Override
    public CommonResponse<VendorRoutingResponseDTO> getById(Long id) {
        VendorRouting entity = vendorRoutingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VendorRouting not found"));
        return CommonResponse.success(mapToDTO(entity), "Fetched successfully");
    }

    @Override
    public CommonResponse<Page<VendorRoutingResponseDTO>> getAll(String subProductId, String createdBy, Pageable pageable) {
        Specification<VendorRouting> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(subProductId)) {
                predicates.add(cb.equal(root.get("subProductId"), subProductId));
            }
            if (StringUtils.hasText(createdBy)) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VendorRouting> page = vendorRoutingRepository.findAll(spec, pageable);
        Page<VendorRoutingResponseDTO> dtoPage = page.map(this::mapToDTO);
        return CommonResponse.success(dtoPage, "Fetched all records");
    }

    private VendorRoutingResponseDTO mapToDTO(VendorRouting entity) {
        VendorRoutingResponseDTO dto = new VendorRoutingResponseDTO();
        dto.setId(entity.getId());
        dto.setSubProductId(entity.getSubProductId());

        // Set vendor objects
        Vendors v1 = entity.getRoutingPriority1();
        Vendors v2 = entity.getRoutingPriority2();
        Vendors v3 = entity.getRoutingPriority3();

        dto.setRoutingPriority1(v1 != null ? v1.getId() : null);
        dto.setRoutingPriority2(v2 != null ? v2.getId() : null);
        dto.setRoutingPriority3(v3 != null ? v3.getId() : null);

        dto.setRoutingPriority1Name(v1 != null ? v1.getVendorName() : null);
        dto.setRoutingPriority2Name(v2 != null ? v2.getVendorName() : null);
        dto.setRoutingPriority3Name(v3 != null ? v3.getVendorName() : null);

        dto.setVolume1(entity.getVolume1());
        dto.setVolume2(entity.getVolume2());
        dto.setVolume3(entity.getVolume3());
        dto.setVolumeNumberOfTxn(entity.getVolumeNumberOfTxn());
        dto.setVolumeValueOfTxn(entity.getVolumeValueOfTxn());

        dto.setValue1(entity.getValue1());
        dto.setValue2(entity.getValue2());
        dto.setValue3(entity.getValue3());
        dto.setValueValueNumberOfTxn(entity.getValueValueNumberOfTxn());
        dto.setValueValueOfTxn(entity.getValueValueOfTxn());

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setEditedBy(entity.getEditedBy());

        return dto;
    }


    private VendorRouting mapToEntity(VendorRoutingRequestDTO dto) {
        VendorRouting entity = new VendorRouting();
        entity.setSubProductId(dto.getSubProductId());

        entity.setRoutingPriority1(getVendorEntity(dto.getRoutingPriority1().getId()));
        entity.setRoutingPriority2(getVendorEntity(dto.getRoutingPriority2().getId()));
        entity.setRoutingPriority3(getVendorEntity(dto.getRoutingPriority3().getId()));

        entity.setVolume1(dto.getVolume1());
        entity.setVolume2(dto.getVolume2());
        entity.setVolume3(dto.getVolume3());
        entity.setVolumeNumberOfTxn(dto.getVolumeNumberOfTxn());
        entity.setVolumeValueOfTxn(dto.getVolumeValueOfTxn());

        entity.setValue1(dto.getValue1());
        entity.setValue2(dto.getValue2());
        entity.setValue3(dto.getValue3());
        entity.setValueValueNumberOfTxn(dto.getValueValueNumberOfTxn());
        entity.setValueValueOfTxn(dto.getValueValueOfTxn());

        entity.setCreatedBy(dto.getCreatedBy());
        entity.setEditedBy(dto.getEditedBy());
        return entity;
    }

    private Vendors getVendorEntity(String vendorId) {
        if (vendorId == null || vendorId.trim().isEmpty()) return null;

        return vendorsRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid vendor ID: " + vendorId));
    }


    private void updateEntity(VendorRouting entity, VendorRoutingRequestDTO dto) {
        entity.setSubProductId(dto.getSubProductId());

        // Resolve vendor references
        entity.setRoutingPriority1(getVendorEntity(dto.getRoutingPriority1().getId()));
        entity.setRoutingPriority2(getVendorEntity(dto.getRoutingPriority2().getId()));
        entity.setRoutingPriority3(getVendorEntity(dto.getRoutingPriority3().getId()));

        entity.setVolume1(dto.getVolume1());
        entity.setVolume2(dto.getVolume2());
        entity.setVolume3(dto.getVolume3());
        entity.setVolumeNumberOfTxn(dto.getVolumeNumberOfTxn());
        entity.setVolumeValueOfTxn(dto.getVolumeValueOfTxn());

        entity.setValue1(dto.getValue1());
        entity.setValue2(dto.getValue2());
        entity.setValue3(dto.getValue3());
        entity.setValueValueNumberOfTxn(dto.getValueValueNumberOfTxn());
        entity.setValueValueOfTxn(dto.getValueValueOfTxn());

        entity.setEditedBy(dto.getEditedBy());
    }


}
