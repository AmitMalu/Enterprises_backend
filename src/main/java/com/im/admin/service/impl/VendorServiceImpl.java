package com.im.admin.service.impl;

import com.im.admin.dto.VendorActiveResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.dto.VendorsDTO;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.service.VendorService;
import com.im.admin.spec.VendorSpecification;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class VendorServiceImpl implements VendorService {

    @Autowired
    private VendorRepository vendorsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;
    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    @Transactional
    public Vendors saveVendor(VendorsDTO dto) {
        if (vendorsRepository.existsByVendorCode(dto.getVendorCode())) {
            throw new IllegalArgumentException("Vendor code '" + dto.getVendorCode() + "' already exists.");
        }

        Vendors vendor = mapToEntity(dto);

        // Generate custom ID
        String generatedId = commonMethods.generateNextIdForTables("vendors", "id");
        vendor.setId(generatedId);

        Vendors savedVendor = vendorsRepository.save(vendor);
        log.info("Vendor [{}] saved successfully.", savedVendor.getId());
        return savedVendor;
    }


    @Override
    @Transactional
    public Vendors updateVendor(String id, VendorsDTO dto) {
        Vendors existingVendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));

        updateEntityFromDto(dto, existingVendor);

        Vendors savedVendor = vendorsRepository.save(existingVendor);
        log.info("Vendor [{}] updated successfully.", savedVendor.getId());
        return savedVendor;
    }


    public void deleteVendor(String id) {
        if (!vendorsRepository.existsById(id)) {
            throw new EntityNotFoundException("Vendor with ID " + id + " not found.");
        }

        vendorsRepository.deleteById(id);
        log.info("Vendor {} deleted successfully.", id);
    }

    @Override
    public List<VendorDropdownResponseDto> getActiveVendorByProductId(Short productId) {
        log.info("Fetching vendors for productId: {}", productId);

        try {
            List<Vendors> vendorList = vendorsRepository.findByProductIdAndActiveTrue(productId);

            if (vendorList.isEmpty()) {
                log.warn("No active vendors found for productId: {}", productId);
            } else {
                log.info("Found {} active vendors for productId: {}", vendorList.size(), productId);
            }

            return vendorList.stream()
                    .map(vendor -> new VendorDropdownResponseDto(
                            vendor.getId(),
                            vendor.getVendorName(),
                            vendor.getVendorCode()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error while fetching vendors for productId: {}", productId, e);
            throw new RuntimeException("Error fetching vendors", e);
        }
    }


    @Override
    public List<VendorActiveResponseDto> getAllActiveVendors() {
        List<Vendors> activeVendors = vendorsRepository.findByActiveTrue();

        return activeVendors.stream()
                .map(v -> new VendorActiveResponseDto(v.getId(), v.getVendorName()))
                .collect(Collectors.toList());
    }

    public Vendors mapToEntity(VendorsDTO dto) {
        Vendors vendor = new Vendors();

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + dto.getProductId()));

        vendor.setProduct(product);
        vendor.setVendorName(dto.getVendorName());
        vendor.setVendorCode(dto.getVendorCode());
        //vendor.setPriority(dto.getPriority());
        vendor.setModesSupported(dto.getModesSupported());
        vendor.setActive(Boolean.TRUE.equals(dto.getActive()));
        vendor.setBankType(dto.getBankType());
        vendor.setWebsite(dto.getWebsite());
        vendor.setLogo(dto.getLogo());
        vendor.setParameter(dto.getParameter());
        vendor.setVendorAccount(dto.getVendorAccount());
        vendor.setUserField1(dto.getUserField1());
        vendor.setUserField2(dto.getUserField2());
        vendor.setUserField3(dto.getUserField3());
        vendor.setCreatedBy(dto.getCreatedBy());

        if (Boolean.TRUE.equals(dto.getIsWhiteLabelAdmin())) {
            vendor.setIsWhiteLabelAdmin(true);
            vendor.setWhiteLabelAdminId(dto.getWhiteLabelAdminId());
        } else {
            vendor.setIsWhiteLabelAdmin(false);
            vendor.setWhiteLabelAdminId("-");
        }

        return vendor;
    }

    public void updateEntityFromDto(VendorsDTO dto, Vendors entity) {
        entity.setVendorName(dto.getVendorName());
        entity.setVendorCode(dto.getVendorCode());
        entity.setActive(dto.getActive());
        //entity.setPriority(dto.getPriority());
        entity.setModesSupported(dto.getModesSupported());
        entity.setBankType(dto.getBankType());
        entity.setWebsite(dto.getWebsite());
        entity.setLogo(dto.getLogo());
        entity.setParameter(dto.getParameter());
        entity.setVendorAccount(dto.getVendorAccount());

        entity.setUserField1(dto.getUserField1());
        entity.setUserField2(dto.getUserField2());
        entity.setUserField3(dto.getUserField3());

        entity.setEditedBy(dto.getEditedBy());
        entity.setEditedOn(LocalDateTime.now());

        if (Boolean.TRUE.equals(dto.getIsWhiteLabelAdmin())) {
            entity.setIsWhiteLabelAdmin(true);
            entity.setWhiteLabelAdminId(dto.getWhiteLabelAdminId());
        } else {
            entity.setIsWhiteLabelAdmin(false);
            entity.setWhiteLabelAdminId("-");
        }
    }

    @Override
    public VendorsDTO getVendorById(String id) {
        Vendors vendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));
        return toDto(vendor);
    }

    @Override
    public Page<VendorsDTO> getAllVendors(Pageable pageable,
                                          List<String> ids,
                                          String vendorName,
                                          String vendorCode,
                                          String productId,
                                          String productCode,
                                          String subProductId,
                                          String subProductCode,
                                          String modesSupported,
                                          Boolean isActive,
                                          String bankType,
                                          String website,
                                          String logo,
                                          String parameter,
                                          String createdBy,
                                          String editedBy,
                                          LocalDate createdOn,
                                          LocalDate editedOn) {

        Specification<Vendors> spec = VendorSpecification.getFilter(
                ids, vendorName, vendorCode, productId, productCode,
                subProductId, subProductCode, modesSupported,
                isActive, bankType, website, logo, parameter,
                createdBy, editedBy, createdOn, editedOn
        );

        Page<Vendors> entityPage = vendorsRepository.findAll(spec, pageable);

        return entityPage.map(this::toDto);
    }

    public VendorsDTO toDto(Vendors vendor) {
        if (vendor == null) return null;

        VendorsDTO dto = new VendorsDTO();
        dto.setId(vendor.getId());
        dto.setVendorName(vendor.getVendorName());
        dto.setVendorCode(vendor.getVendorCode());

        if (vendor.getProduct() != null) {
            dto.setProductId(vendor.getProduct().getId());
            dto.setProductName(vendor.getProduct().getName());
        }

        dto.setModesSupported(vendor.getModesSupported());
        dto.setActive(vendor.getActive());
        dto.setBankType(vendor.getBankType());
        dto.setWebsite(vendor.getWebsite());
        dto.setLogo(vendor.getLogo());
        dto.setParameter(vendor.getParameter());

        dto.setVendorAccount(vendor.getVendorAccount());

        dto.setIsWhiteLabelAdmin(vendor.getIsWhiteLabelAdmin());
        dto.setWhiteLabelAdminId(vendor.getWhiteLabelAdminId());

        dto.setUserField1(vendor.getUserField1());
        dto.setUserField2(vendor.getUserField2());
        dto.setUserField3(vendor.getUserField3());

        dto.setCreatedBy(vendor.getCreatedBy());
        dto.setCreatedOn(vendor.getCreatedOn());
        dto.setEditedBy(vendor.getEditedBy());
        dto.setEditedOn(vendor.getEditedOn());

        return dto;
    }


}
