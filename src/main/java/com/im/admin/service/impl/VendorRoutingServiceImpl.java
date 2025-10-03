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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public CommonResponse<Page<VendorRoutingResponseDTO>> getAll(Map<String, String> filters, Pageable pageable) {
        Specification<VendorRouting> spec = buildSpecification(filters);

        Page<VendorRouting> page = vendorRoutingRepository.findAll(spec, pageable);
        log.debug("Fetched VendorRouting: totalElements={}", page.getTotalElements());

        Page<VendorRoutingResponseDTO> dtoPage = page.map(this::mapToDTO);
        return CommonResponse.success(dtoPage, "Fetched VendorRouting records successfully");
    }

    private Specification<VendorRouting> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((field, value) -> {
                if (!StringUtils.hasText(value)) return;

                switch (field) {
                    case "id" -> predicates.add(cb.equal(root.get("id"), convertType(Long.class, value)));

                    case "subProductId" -> {
                        Join<Object, Object> subJoin = root.join("subProduct", JoinType.INNER);
                        predicates.add(cb.equal(subJoin.get("id"), value)); // String
                    }
                    case "subProductName" -> {
                        Join<Object, Object> subJoin = root.join("subProduct", JoinType.INNER);
                        predicates.add(cb.like(cb.lower(subJoin.get("subProductTitle")), "%" + value.toLowerCase() + "%"));
                    }

                    case "routingPriority1" -> {
                        Join<Object, Object> v1Join = root.join("routingPriority1", JoinType.INNER);
                        predicates.add(cb.equal(v1Join.get("id"), value)); // <-- String
                    }

                    case "routingPriority1Name" -> {
                        Join<Object, Object> v1Join = root.join("routingPriority1", JoinType.INNER);
                        predicates.add(cb.like(cb.lower(v1Join.get("vendorName")), "%" + value.toLowerCase() + "%"));
                    }


                    case "routingPriority2" -> {
                        Join<Object, Object> v2Join = root.join("routingPriority2", JoinType.INNER);
                        predicates.add(cb.equal(v2Join.get("id"), value));
                    }

                    case "routingPriority2Name" -> {
                        Join<Object, Object> v2Join = root.join("routingPriority2", JoinType.INNER);
                        predicates.add(cb.like(cb.lower(v2Join.get("vendorName")), "%" + value.toLowerCase() + "%"));
                    }


                    case "routingPriority3" -> {
                        Join<Object, Object> v3Join = root.join("routingPriority3", JoinType.INNER);
                        predicates.add(cb.equal(v3Join.get("id"), value));
                    }

                    case "routingPriority3Name" -> {
                        Join<Object, Object> v3Join = root.join("routingPriority3", JoinType.INNER);
                        predicates.add(cb.like(cb.lower(v3Join.get("vendorName")), "%" + value.toLowerCase() + "%"));
                    }

                    case "createdOn" -> {
                        try {
                            LocalDate date = LocalDate.parse(value);
                            predicates.add(cb.equal(cb.function("DATE", LocalDate.class, root.get("createdOn")), date));
                        } catch (Exception e) {
                            log.warn("Invalid createdOn filter value: {}", value, e);
                        }
                    }

                    case "editedOn" -> {
                        try {
                            LocalDate date = LocalDate.parse(value);
                            predicates.add(cb.equal(cb.function("DATE", LocalDate.class, root.get("editedOn")), date));
                        } catch (Exception e) {
                            log.warn("Invalid editedOn filter value: {}", value, e);
                        }
                    }


                    default -> {
                        try {
                            if (root.get(field).getJavaType().equals(String.class)) {
                                predicates.add(cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
                            } else {
                                predicates.add(cb.equal(root.get(field),
                                        convertType(root.get(field).getJavaType(), value)));
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn("Filter field '{}' not found on VendorRouting entity, ignoring.", field);
                        }
                    }
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Object convertType(Class<?> targetType, String value) {
        if (Long.class.equals(targetType) || long.class.equals(targetType)) {
            return Long.valueOf(value);
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Integer.valueOf(value);
        }
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            return Boolean.valueOf(value);
        }
        return value;
    }

    private VendorRoutingResponseDTO mapToDTO(VendorRouting entity) {
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
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setEditedOn(entity.getEditedOn());



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
        entity.setCreatedOn(dto.getCreatedOn());
        entity.setEditedOn(dto.getEditedOn());

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
        entity.setEditedBy(dto.getEditedBy());
        entity.setEditedOn(LocalDateTime.now());


        log.debug("Updated entity: {}", entity);
    }



}
