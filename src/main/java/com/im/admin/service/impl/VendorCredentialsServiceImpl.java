package com.im.admin.service.impl;

import com.im.admin.dto.VendorCredentialsDTO;
import com.im.admin.dto.VendorCredentialsResponseDTO;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.service.VendorCredentialsService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
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
        log.info("Saving VendorCredentials for username: {}", dto.getUsername());
        VendorCredentials entity = mapToEntity(dto);
        VendorCredentials saved = repository.save(entity);
        log.debug("Saved VendorCredentials entity: {}", saved);
        return saved;
    }

    @Override
    @Transactional
    public VendorCredentialsResponseDTO updateVendorCredentials(Long id, VendorCredentialsDTO dto) {
        log.info("Updating VendorCredentials with ID: {}", id);

        VendorCredentials existing = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("VendorCredentials not found with ID: {}", id);
                    return new RuntimeException("VendorCredentials not found with id: " + id);
                });

        Vendors vendor = vendorsRepository.findById(dto.getVendorId().getId())
                .orElseThrow(() -> {
                    log.error("Invalid vendor ID: {}", dto.getVendorId().getId());
                    return new IllegalArgumentException("Invalid vendor ID: " + dto.getVendorId().getId());
                });

        Products product = productsRepository.findById(dto.getProductId().getId())
                .orElseThrow(() -> {
                    log.error("Invalid product ID: {}", dto.getProductId().getId());
                    return new IllegalArgumentException("Invalid product ID: " + dto.getProductId().getId());
                });

        log.debug("Updating fields for VendorCredentials ID: {}", id);
        // [Set fields as before]
        // Set updated fields
        existing.setVendor(vendor);
        existing.setProduct(product);
        existing.setBaseUrlUat(dto.getBaseUrlUat());
        existing.setBaseUrlProd(dto.getBaseUrlProd());
        existing.setTokenUrlUat(dto.getTokenUrlUat());
        existing.setTokenUrlProd(dto.getTokenUrlProd());
        existing.setToken(dto.getToken());
        existing.setTokenType(dto.getTokenType());
        existing.setClientId(dto.getClientId());
        existing.setClientSecret(dto.getClientSecret());
        existing.setSaltKey(dto.getSaltKey());
        existing.setUsername(dto.getUsername());
        existing.setPassword(dto.getPassword());
        existing.setIsActive(dto.getIsActive());
        existing.setIsWhiteLabelAdmin(dto.getIsWhiteLabelAdmin());
        existing.setWhiteLabelAdminId(dto.getWhiteLabelAdminId());
        existing.setUserField1(dto.getUserField1());
        existing.setUserField2(dto.getUserField2());
        existing.setUserField3(dto.getUserField3());
        existing.setEditedBy(dto.getEditedBy());

        VendorCredentials saved = repository.save(existing);
        log.debug("Updated VendorCredentials entity: {}", saved);
        return mapToDTO(saved);
    }

    @Override
    public Page<VendorCredentialsResponseDTO> getAllVendorCredentials(
            String productName, String vendorName, String whiteLabelAdminName,
            String username, Boolean isActive, String createdBy, Pageable pageable) {

        log.info("Fetching VendorCredentials with filters - username: {}, isActive: {}, createdBy: {}, productName: {}, vendorName: {}, whiteLabelAdminName: {}",
                username, isActive, createdBy, productName, vendorName, whiteLabelAdminName);

        Specification<VendorCredentials> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (StringUtils.hasText(createdBy)) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(productName)) {
                predicates.add(cb.like(cb.lower(root.get("product").get("name")), "%" + productName.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(vendorName)) {
                predicates.add(cb.like(cb.lower(root.get("vendor").get("vendorName")), "%" + vendorName.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(whiteLabelAdminName)) {
                // Join white label admin if modeled, or skip
                // predicates.add(cb.like(...)) if applicable
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VendorCredentials> page = repository.findAll(spec, pageable);
        log.debug("Fetched {} VendorCredentials records", page.getTotalElements());

        return page.map(entity -> {
            VendorCredentialsResponseDTO dto = mapToDTO(entity);
            log.trace("Mapped VendorCredentials entity to DTO: {}", dto);
            return dto;
        });
    }

    @Override
    public VendorCredentialsResponseDTO getVendorCredentialsById(Long id) {
        log.info("Fetching VendorCredentials by ID: {}", id);
        VendorCredentials entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("VendorCredentials not found with ID: {}", id);
                    return new RuntimeException("VendorCredentials not found with id: " + id);
                });
        return mapToDTO(entity);
    }


    private VendorCredentials mapToEntity(VendorCredentialsDTO dto) {
        Vendors vendor = vendorsRepository.findById(dto.getVendorId().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vendor ID: " + dto.getVendorId().getId()));

        Products product = productsRepository.findById(dto.getProductId().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + dto.getProductId().getId()));

        VendorCredentials entity = new VendorCredentials();
        entity.setVendor(vendor);
        entity.setProduct(product);

        entity.setToken(dto.getToken());
        entity.setTokenType(dto.getTokenType());
        entity.setBaseUrlUat(dto.getBaseUrlUat());
        entity.setBaseUrlProd(dto.getBaseUrlProd());
        entity.setTokenUrlUat(dto.getTokenUrlUat());
        entity.setTokenUrlProd(dto.getTokenUrlProd());
        entity.setClientId(dto.getClientId());
        entity.setClientSecret(dto.getClientSecret());
        entity.setSaltKey(dto.getSaltKey());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setIsActive(dto.getIsActive());
        entity.setIsWhiteLabelAdmin(dto.getIsWhiteLabelAdmin());
        entity.setWhiteLabelAdminId(dto.getWhiteLabelAdminId());
        entity.setUserField1(dto.getUserField1());
        entity.setUserField2(dto.getUserField2());
        entity.setUserField3(dto.getUserField3());
        entity.setTokenType(dto.getTokenType());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setEditedBy(dto.getEditedBy());

        return entity;
    }


    private VendorCredentialsResponseDTO mapToDTO(VendorCredentials entity) {
        VendorCredentialsResponseDTO dto = new VendorCredentialsResponseDTO();

        // Use manual mapping instead of BeanUtils to avoid lazy-loading issues
        dto.setId(entity.getId());
        dto.setBaseUrlUat(entity.getBaseUrlUat());
        dto.setBaseUrlProd(entity.getBaseUrlProd());
        dto.setToken(entity.getToken());
        dto.setTokenType(entity.getTokenType());
        dto.setTokenUrlUat(entity.getTokenUrlUat());
        dto.setTokenUrlProd(entity.getTokenUrlProd());
        dto.setClientId(entity.getClientId());
        dto.setClientSecret(entity.getClientSecret());
        dto.setSaltKey(entity.getSaltKey());
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        dto.setIsActive(entity.getIsActive());
        dto.setIsWhiteLabelAdmin(entity.getIsWhiteLabelAdmin());
        dto.setWhiteLabelAdminId(entity.getWhiteLabelAdminId());
        dto.setUserField1(entity.getUserField1());
        dto.setUserField2(entity.getUserField2());
        dto.setUserField3(entity.getUserField3());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setEditedBy(entity.getEditedBy());

        // Vendor info
        if (entity.getVendor() != null) {
            dto.setVendorId(entity.getVendor().getId());
            dto.setVendorName(entity.getVendor().getVendorName());
        }

        // Product info
        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductName(entity.getProduct().getName());
        }

        // Whitelabel name (if present)
        if (entity.getWhiteLabelAdminId() != null) {
            dto.setWhiteLabelAdminId(entity.getWhiteLabelAdminId());
            whiteLabelsRepository.findById(entity.getWhiteLabelAdminId()).ifPresent(wl ->
                    dto.setWhiteLabelAdminName(wl.getName()));
        }

        return dto;
    }

}

