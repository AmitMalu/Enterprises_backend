package com.im.admin.service.impl;

import com.im.admin.dto.VendorCredentialsDTO;
import com.im.admin.dto.VendorCredentialsResponseDTO;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.service.VendorCredentialsService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VendorCredentialsServiceImpl implements VendorCredentialsService {

    @Autowired
    private VendorCredentialsRepository repository;

    @Autowired
    private VendorRepository vendorsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Override
    @Transactional
    public VendorCredentials saveVendorCredentials(VendorCredentialsDTO dto) {
        VendorCredentials entity = new VendorCredentials();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedBy(dto.getCreatedBy());
        return repository.save(entity);
    }


    @Override
    @Transactional
    public VendorCredentials updateVendorCredentials(Long id, VendorCredentialsDTO dto) {
        VendorCredentials existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("VendorCredentials not found with id: " + id));

        BeanUtils.copyProperties(dto, existing, "id", "createdOn", "createdBy");
        existing.setEditedBy(dto.getEditedBy());
        return repository.save(existing);
    }


    @Override
    public Page<VendorCredentialsResponseDTO> getAllVendorCredentials(
            String productName,
            String vendorName,
            String whiteLabelAdminName,
            String username,
            Boolean isActive,
            String createdBy,
            Pageable pageable) {

        // DB filtering for username, isActive, createdBy
        Specification<VendorCredentials> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (createdBy != null && !createdBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VendorCredentials> page = repository.findAll(spec, pageable);

        // Map to DTOs and apply in-memory filtering for names
        List<VendorCredentialsResponseDTO> filtered = page.getContent().stream().map(entity -> {
            VendorCredentialsResponseDTO dto = new VendorCredentialsResponseDTO();
            dto.setId(entity.getId());
            dto.setUsername(entity.getUsername());
            dto.setClientId(entity.getClientId());
            dto.setClientSecret(entity.getClientSecret());
            dto.setBaseUrlUat(entity.getBaseUrlUat());
            dto.setBaseUrlProd(entity.getBaseUrlProd());
            dto.setIsActive(entity.getIsActive());
            dto.setUserField1(entity.getUserField1());
            dto.setUserField2(entity.getUserField2());
            dto.setUserField3(entity.getUserField3());
            dto.setCreatedBy(entity.getCreatedBy());
            dto.setCreatedOn(entity.getCreatedOn());
            dto.setEditedBy(entity.getEditedBy());
            dto.setEditedOn(entity.getEditedOn());

            dto.setVendorId(entity.getVendorId());
            vendorsRepository.findById(entity.getVendorId()).ifPresent(v ->
                    dto.setVendorName(v.getVendorName()));

            dto.setProductId(entity.getProductId());
            productsRepository.findById(entity.getProductId()).ifPresent(p ->
                    dto.setProductName(p.getName()));

            dto.setWhiteLabelAdminId(entity.getWhiteLabelAdminId());
            if (entity.getWhiteLabelAdminId() != null) {
                whiteLabelsRepository.findById(entity.getWhiteLabelAdminId()).ifPresent(wl ->
                        dto.setWhiteLabelAdminName(wl.getName()));
            }

            return dto;
        }).filter(dto -> {
            return (productName == null || (dto.getProductName() != null && dto.getProductName().toLowerCase().contains(productName.toLowerCase()))) &&
                    (vendorName == null || (dto.getVendorName() != null && dto.getVendorName().toLowerCase().contains(vendorName.toLowerCase()))) &&
                    (whiteLabelAdminName == null || (dto.getWhiteLabelAdminName() != null && dto.getWhiteLabelAdminName().toLowerCase().contains(whiteLabelAdminName.toLowerCase())));
        }).toList();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<VendorCredentialsResponseDTO> paged = (start > end) ? List.of() : filtered.subList(start, end);

        return new PageImpl<>(paged, pageable, filtered.size());
    }

    public static Specification<VendorCredentials> filter(
            String vendorId, Short productId, Boolean isActive, String whiteLabelAdminId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (vendorId != null && !vendorId.isEmpty()) {
                predicates.add(cb.equal(root.get("vendorId"), vendorId));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (whiteLabelAdminId != null && !whiteLabelAdminId.isEmpty()) {
                predicates.add(cb.equal(root.get("whiteLabelAdminId"), whiteLabelAdminId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    @Override
    public VendorCredentials getVendorCredentialsById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("VendorCredentials not found with id: " + id));
    }
}

