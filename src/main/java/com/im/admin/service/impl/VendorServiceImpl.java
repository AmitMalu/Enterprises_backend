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
import com.im.admin.response.CommonResponse;
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
        log.info("Attempting to save vendor with code: {}", dto.getVendorCode());

        if (vendorsRepository.existsByVendorCode(dto.getVendorCode())) {
            log.warn("Vendor code '{}' already exists. Aborting save operation.", dto.getVendorCode());
            throw new IllegalArgumentException("Vendor code '" + dto.getVendorCode() + "' already exists.");
        }

        Vendors vendor = mapToEntity(dto);
        log.debug("Mapped DTO to entity: {}", vendor);

        // Generate custom ID
        String generatedId = commonMethods.generateNextIdForTables("vendors", "id");
        vendor.setId(generatedId);
        log.info("Generated vendor ID: {}", generatedId);

        Vendors savedVendor = vendorsRepository.save(vendor);
        log.info("Vendor [{}] saved successfully with code: {}", savedVendor.getId(), savedVendor.getVendorCode());

        return savedVendor;
    }


    @Override
    @Transactional
    public Vendors updateVendor(String id, VendorsDTO dto) {
        log.info("Initiating update for vendor ID: {}", id);

        Vendors existingVendor = vendorsRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vendor with ID {} not found. Cannot proceed with update.", id);
                    return new EntityNotFoundException("Vendor with ID " + id + " not found.");
                });

        log.debug("Existing vendor before update: {}", existingVendor);

        updateEntityFromDto(dto, existingVendor);
        log.debug("Vendor entity after applying updates from DTO: {}", existingVendor);

        Vendors savedVendor = vendorsRepository.save(existingVendor);
        log.info("Vendor [{}] updated successfully with code: {}", savedVendor.getId(), savedVendor.getVendorCode());

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
        log.info("Fetching all active vendors");

        List<Vendors> activeVendors = vendorsRepository.findByActiveTrue();
        log.debug("Found {} active vendors", activeVendors.size());

        List<VendorActiveResponseDto> response = activeVendors.stream()
                .map(v -> new VendorActiveResponseDto(v.getId(), v.getVendorName()))
                .collect(Collectors.toList());

        log.info("Returning {} active vendor DTOs", response.size());
        return response;
    }

    public Vendors mapToEntity(VendorsDTO dto) {
        log.info("Mapping VendorsDTO to entity for vendorCode: {}", dto.getVendorCode());

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", dto.getProductId());
                    return new RuntimeException("Product not found with ID: " + dto.getProductId());
                });

        Vendors vendor = new Vendors();
        vendor.setProduct(product);
        vendor.setVendorName(dto.getVendorName());
        vendor.setVendorCode(dto.getVendorCode());
        // vendor.setPriority(dto.getPriority());
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

        log.debug("Mapped entity: {}", vendor);
        return vendor;
    }

    public void updateEntityFromDto(VendorsDTO dto, Vendors entity) {
        log.info("Updating vendor entity [{}] with new DTO data", entity.getId());

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", dto.getProductId());
                    return new RuntimeException("Product not found with ID: " + dto.getProductId());
                });

        entity.setProduct(product);
        entity.setVendorName(dto.getVendorName());
        entity.setVendorCode(dto.getVendorCode());
        entity.setActive(dto.getActive());
        // entity.setPriority(dto.getPriority());
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

        log.debug("Updated vendor entity: {}", entity);
    }


    @Override
    public VendorsDTO getVendorById(String id) {
        log.info("Fetching vendor by ID: {}", id);

        Vendors vendor = vendorsRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vendor with ID {} not found", id);
                    return new EntityNotFoundException("Vendor with ID " + id + " not found.");
                });

        VendorsDTO dto = toDto(vendor);
        log.debug("Mapped vendor entity to DTO: {}", dto);
        return dto;
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
                                          Boolean active,
                                          String bankType,
                                          String website,
                                          String logo,
                                          String parameter,
                                          String createdBy,
                                          String editedBy,
                                          LocalDate createdOn,
                                          LocalDate editedOn) {

        log.info("Fetching vendors with filters - vendorName: {}, vendorCode: {}, productId: {}, isActive: {}, page: {}, size: {}",
                vendorName, vendorCode, productId, active, pageable.getPageNumber(), pageable.getPageSize());

        Specification<Vendors> spec = VendorSpecification.getFilter(
                ids, vendorName, vendorCode, productId, productCode,
                subProductId, subProductCode, modesSupported,
                active, bankType, website, logo, parameter,
                createdBy, editedBy, createdOn, editedOn
        );

        Page<Vendors> entityPage = vendorsRepository.findAll(spec, pageable);
        log.debug("Found {} vendors matching criteria", entityPage.getTotalElements());

        Page<VendorsDTO> dtoPage = entityPage.map(this::toDto);
        log.info("Returning {} vendor DTOs", dtoPage.getContent().size());

        return dtoPage;
    }


    public VendorsDTO toDto(Vendors vendor) {
        if (vendor == null) {
            log.warn("Attempted to convert null Vendor entity to DTO.");
            return null;
        }

        log.debug("Converting Vendor entity [{}] to DTO", vendor.getId());

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

        log.debug("Vendor DTO created: {}", dto);
        return dto;
    }

    @Override
    public CommonResponse<List<VendorDropdownResponseDto>> getVendorsBySubProduct(String subProductId) {

        SubProducts subProduct = subProductsRepository.findById(subProductId)
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductId));

        Products product = subProduct.getProduct();

        List<Vendors> vendors = vendorsRepository.findByProduct(product);

        if (vendors.isEmpty()) {
            throw new RuntimeException("No vendors found for product id: " + product.getId());
        }

        List<VendorDropdownResponseDto> dtoList = vendors.stream().map(v -> {
            VendorDropdownResponseDto dto = new VendorDropdownResponseDto();
            dto.setId(v.getId());
            dto.setVendorName(v.getVendorName());
            dto.setVendorCode(v.getVendorCode());
            return dto;
        }).toList();

        return CommonResponse.success(dtoList, "Vendors fetched successfully");
    }

}
