package com.im.admin.service.impl;

import com.im.admin.entities.vendor.Vendors;
import com.im.admin.exception.ResourceNotFoundException;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@Slf4j
public class VendorServiceImpl implements VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    public VendorServiceImpl(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public Vendors saveVendor(Vendors vendor) {
        vendor.setCreatedOn(LocalDateTime.now());
        vendor.setCreatedBy("SYSTEM"); // Replace with authenticated user
        log.info("Saving new vendor: {}", vendor);
        return vendorRepository.save(vendor);
    }

    @Override
    public Vendors updateVendor(Long id, Vendors vendor) {
        Vendors existingVendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id " + id));

        existingVendor.setInfo(vendor.getInfo());
        existingVendor.setVendorAccount(vendor.getVendorAccount());
        existingVendor.setEditedOn(LocalDateTime.now());
        existingVendor.setEditedBy("SYSTEM"); // Replace with authenticated user
        log.info("Updating vendor with ID {}: {}", id, existingVendor);

        return vendorRepository.save(existingVendor);
    }

    @Override
    public Page<Vendors> getAllVendors(Pageable pageable) {
        log.info("Fetching all vendors with pagination");
        return vendorRepository.findAll(pageable);
    }

    @Override
    public Vendors getVendorById(Long id) {
        log.info("Fetching vendor with ID: {}", id);
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id " + id));
    }

    @Override
    public void deleteVendor(Long id) {
        log.info("Deleting vendor with ID: {}", id);
        Vendors vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id " + id));
        vendorRepository.delete(vendor);
    }
}
