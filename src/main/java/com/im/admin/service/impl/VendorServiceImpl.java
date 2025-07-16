package com.im.admin.service.impl;

import com.im.admin.dto.VendorActiveResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
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
    public Vendors saveVendor(Vendors vendor) {
        if (vendorsRepository.existsByVendorCode(vendor.getVendorCode())) {
            throw new IllegalArgumentException("Vendor with this code already exists");
        }

        // Fetch and set Product
        Products product = productsRepository.findById(Short.valueOf(vendor.getProductId()))
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + vendor.getProductId()));

        // Fetch SubProduct by ID
        //SubProducts subProduct = subProductsRepository.findById(vendor.getSubProductId())
          //      .orElseThrow(() -> new IllegalArgumentException("Invalid SubProduct ID: " + vendor.getSubProductId()));

        // Set SubProductCode from the fetched entity
        //vendor.setSubProductCode(subProduct.getSubProductCode());

        vendor.setId(commonMethods.generateNextIdForTables("vendors", "id"));

        if (!Boolean.TRUE.equals(vendor.getIsWhiteLabelAdmin())) {
            vendor.setIsWhiteLabelAdmin(false);
            vendor.setWhiteLabelAdminId("-");
        }

        Vendors savedVendor = vendorsRepository.save(vendor);
        log.info("Vendor {} saved successfully.", savedVendor.getId());
        return savedVendor;
    }

    public Vendors getVendorById(String id) {
        return vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));
    }

    public Page<Vendors> getAllVendors(Pageable pageable, List<String> id, String vendorName,
                                       String vendorCode, String productId,
                                       String productCode, String subProductId,
                                       String subProductCode, String modesSupported,
                                       Integer priority, Boolean isActive,
                                       String bankType, String website,
                                       String logo, String parameter,
                                       String createdBy, String editedBy,
                                       LocalDate createdOn,
                                       LocalDate editedOn) {

        Specification<Vendors> spec = VendorSpecification.getFilter(
                id, vendorName, vendorCode, productId, productCode,
                subProductId, subProductCode, modesSupported, priority, isActive,
                bankType, website, logo, parameter, createdBy, editedBy,
                createdOn, editedOn
        );

        return vendorsRepository.findAll(spec, pageable);
    }


    public Vendors updateVendor(String id, Vendors updatedVendor) {
        Vendors existingVendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));

        //SubProducts subProduct = subProductsRepository.findById(String.valueOf(updatedVendor.getSubProductId()))
         //       .orElseThrow(() -> new RuntimeException("SubProduct not found with ID: " + updatedVendor.getSubProductId()));

        existingVendor.setActive(updatedVendor.getActive());
        existingVendor.setVendorName(updatedVendor.getVendorName());
        existingVendor.setVendorCode(updatedVendor.getVendorCode());
        existingVendor.setVendorAccount(updatedVendor.getVendorAccount());
        existingVendor.setModesSupported(updatedVendor.getModesSupported());
        existingVendor.setEditedOn(LocalDateTime.now());
        existingVendor.setBankType(updatedVendor.getBankType());
        existingVendor.setWebsite(updatedVendor.getWebsite());
        existingVendor.setLogo(updatedVendor.getLogo());
        existingVendor.setParameter(updatedVendor.getParameter());
        existingVendor.setVendorAccount(updatedVendor.getVendorAccount());
       //existingVendor.setSubProductId(updatedVendor.getSubProductId());
       // existingVendor.setSubProductCode(subProduct.getSubProductCode());
        existingVendor.setUserField1(updatedVendor.getUserField1());
        existingVendor.setUserField2(updatedVendor.getUserField2());
        existingVendor.setUserField3(updatedVendor.getUserField3());
        existingVendor.setEditedBy(updatedVendor.getEditedBy());

        if (Boolean.TRUE.equals(updatedVendor.getIsWhiteLabelAdmin())) {
            existingVendor.setIsWhiteLabelAdmin(true);
            existingVendor.setWhiteLabelAdminId(updatedVendor.getWhiteLabelAdminId());
        }else {
            existingVendor.setIsWhiteLabelAdmin(false);
            existingVendor.setWhiteLabelAdminId("-");
        }

        Vendors savedVendor = vendorsRepository.save(existingVendor);
        log.info("Vendor {} updated successfully.", savedVendor.getId());
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
    public List<VendorDropdownResponseDto> getActiveVendorByProductId(String productId) {
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

}
