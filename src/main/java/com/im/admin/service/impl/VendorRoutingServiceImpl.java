package com.im.admin.service.impl;

import com.im.admin.dto.VendorRoutingRequestDTO;
import com.im.admin.dto.VendorRoutingResponseDTO;
import com.im.admin.dto.VendorsDTO;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorRoutingService;
import com.im.admin.service.VendorService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VendorRoutingServiceImpl implements VendorRoutingService {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRepository vendorsRepository;


    @Override
    @Transactional
    public CommonResponse<VendorRoutingResponseDTO> save(VendorRoutingRequestDTO request) {
        log.info("Received request to save VendorRouting: {}", request);

        VendorRouting entity = mapToEntity(request);
        VendorRouting saved = vendorRoutingRepository.save(entity);

        log.debug("VendorRouting entity saved: {}", saved);
        return CommonResponse.success(mapToDTO(saved), "Saved successfully");
    }

    @Override
    public CommonResponse<VendorRoutingResponseDTO> update(Long id, VendorRoutingRequestDTO request) {
        log.info("Received request to update VendorRouting with ID: {}", id);

        VendorRouting existing = vendorRoutingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("VendorRouting with ID {} not found", id);
                    return new RuntimeException("VendorRouting not found");
                });

        log.debug("Existing VendorRouting before update: {}", existing);
        updateEntity(existing, request);

        VendorRouting updated = vendorRoutingRepository.save(existing);
        log.debug("VendorRouting entity updated: {}", updated);

        return CommonResponse.success(mapToDTO(updated), "Updated successfully");
    }


    @Override
    public CommonResponse<VendorRoutingResponseDTO> getById(Long id) {
        log.info("Fetching VendorRouting by ID: {}", id);

        VendorRouting entity = vendorRoutingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("VendorRouting not found for ID: {}", id);
                    return new RuntimeException("VendorRouting not found");
                });

        log.debug("VendorRouting entity fetched: {}", entity);
        return CommonResponse.success(mapToDTO(entity), "Fetched successfully");
    }

    @Override
    public CommonResponse<Page<VendorRoutingResponseDTO>> getAll(String subProductId, String createdBy, Pageable pageable) {
        log.info("Fetching all VendorRouting records with filters - subProductId: {}, createdBy: {}", subProductId, createdBy);

        Specification<VendorRouting> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(subProductId)) {
                log.debug("Applying filter: subProductId = {}", subProductId);
                Join<VendorRouting, SubProducts> subProductJoin = root.join("subProduct", JoinType.INNER);
                predicates.add(cb.equal(subProductJoin.get("id"), subProductId));
            }

            if (StringUtils.hasText(createdBy)) {
                log.debug("Applying filter: createdBy = {}", createdBy);
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VendorRouting> page = vendorRoutingRepository.findAll(spec, pageable);
        log.debug("VendorRouting page fetched: totalElements = {}", page.getTotalElements());

        Page<VendorRoutingResponseDTO> dtoPage = page.map(this::mapToDTO);
        return CommonResponse.success(dtoPage, "Fetched all records");
    }

    private VendorRoutingResponseDTO mapToDTO(VendorRouting entity) {
        log.debug("Mapping VendorRouting entity to DTO: {}", entity);

        VendorRoutingResponseDTO dto = new VendorRoutingResponseDTO();
        dto.setId(entity.getId());

        if (entity.getSubProduct() != null) {
            dto.setSubProductId(entity.getSubProduct().getId());
            dto.setSubProductName(entity.getSubProduct().getSubProductTitle());
        }

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

        log.debug("Mapped DTO: {}", dto);
        return dto;
    }

    private VendorRouting mapToEntity(VendorRoutingRequestDTO dto) {
        log.debug("Mapping DTO to VendorRouting entity: {}", dto);

        VendorRouting entity = new VendorRouting();

        SubProducts subProduct = subProductsRepository.findById(dto.getSubProductId())
                .orElseThrow(() -> {
                    log.error("SubProduct not found with ID: {}", dto.getSubProductId());
                    return new RuntimeException("SubProduct not found with ID: " + dto.getSubProductId());
                });
        entity.setSubProduct(subProduct);

        entity.setRoutingPriority1(getVendorEntitySafe(dto.getRoutingPriority1()));
        entity.setRoutingPriority2(getVendorEntitySafe(dto.getRoutingPriority2()));
        entity.setRoutingPriority3(getVendorEntitySafe(dto.getRoutingPriority3()));

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

        log.debug("Mapped entity: {}", entity);
        return entity;
    }

    private Vendors getVendorEntitySafe(VendorsDTO dto) {
        if (dto == null || dto.getId() == null || dto.getId().trim().isEmpty()) {
            log.warn("VendorDTO is null or has empty ID: {}", dto);
            return null;
        }

        return vendorsRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    log.error("Invalid vendor ID: {}", dto.getId());
                    return new IllegalArgumentException("Invalid vendor ID: " + dto.getId());
                });
    }

    private void updateEntity(VendorRouting entity, VendorRoutingRequestDTO dto) {
        log.debug("Updating VendorRouting entity with DTO: {}", dto);

        SubProducts subProduct = subProductsRepository.findById(dto.getSubProductId())
                .orElseThrow(() -> {
                    log.error("SubProduct not found with ID: {}", dto.getSubProductId());
                    return new RuntimeException("SubProduct not found with ID: " + dto.getSubProductId());
                });
        entity.setSubProduct(subProduct);

        entity.setRoutingPriority1(getVendorEntitySafe(dto.getRoutingPriority1()));
        entity.setRoutingPriority2(getVendorEntitySafe(dto.getRoutingPriority2()));
        entity.setRoutingPriority3(getVendorEntitySafe(dto.getRoutingPriority3()));

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

        log.debug("Updated entity: {}", entity);
    }



}
