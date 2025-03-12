package com.im.admin.service.impl;

import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.service.VendorService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class VendorServiceImpl implements VendorService {

    @Autowired
    private VendorRepository vendorsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public Vendors saveVendor(Vendors vendor) {
        if (vendorsRepository.existsByVendorCode(vendor.getVendorCode())) {
            throw new IllegalArgumentException("Vendor with this code already exists");
        }
        vendor.setId(commonMethods.generateNextIdForTables("vendors", "id"));
        Vendors savedVendor = vendorsRepository.save(vendor);
        log.info("Vendor {} saved successfully.", savedVendor.getId());
        return savedVendor;
    }

    public Vendors getVendorById(String id) {
        return vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));
    }

    public Page<Vendors> getAllVendors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vendorsRepository.findAll(pageable);
    }

    public Vendors updateVendor(String id, Vendors updatedVendor) {
        Vendors existingVendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vendor with ID " + id + " not found."));

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
    public List<VendorDropdownResponseDto> getActiveVendorBySubProductId(String subProductId) {
        log.info("Fetching vendors for subProductId: {}", subProductId);

        try {
            List<Vendors> vendorList = vendorsRepository.findBySubProductIdAndActiveTrue(subProductId);

            if (vendorList.isEmpty()) {
                log.warn("No active vendors found for subProductId: {}", subProductId);
            } else {
                log.info("Found {} active vendors for subProductId: {}", vendorList.size(), subProductId);
            }

            return vendorList.stream()
                    .map(vendor -> new VendorDropdownResponseDto(vendor.getId(), vendor.getVendorName(), vendor.getVendorCode()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error while fetching vendors for subProductId: {}", subProductId, e);
            throw new RuntimeException("Error fetching vendors", e);
        }
    }
}
