package com.im.admin.service.impl;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Vendor;
import com.im.admin.repositories.VendorRepository;
import com.im.admin.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Service
@Slf4j
public class VendorServiceImpl implements VendorService {

    @Autowired
    VendorRepository vendorRepository;


    @Override
    public Vendor saveVendor(Vendor vendor) {
        log.info("Saving new vendor: {}", vendor.getApiVendorName());
        Vendor save = vendorRepository.save(vendor);
        return save;

    }

    @Override
    public void deleteVendor(Long id) {
        log.info("Deleting vendor with ID: {}", id);
        vendorRepository.deleteById(id);
    }

    @Override
    public Optional<Vendor> getVendorById(Long id) {
        log.info("Fetching vendor with ID: {}", id);
        return vendorRepository.findById(id);
    }


    @Override
    public Vendor updateVendor(Long id, Vendor vendor) {
        log.info("Updating vendor with ID: {}", id);
        Optional<Vendor> existingVendor = vendorRepository.findById(id);
        if (existingVendor.isPresent()) {
            Vendor updatedVendor = existingVendor.get();
            updatedVendor.setApiVendorName(vendor.getApiVendorName());
            updatedVendor.setApiVendorClass(vendor.getApiVendorClass());
            updatedVendor.setActive(vendor.getActive());
            updatedVendor.setBankType(vendor.getBankType());
            updatedVendor.setModesSupported(vendor.getModesSupported());
            return vendorRepository.save(updatedVendor);
        } else {
            throw new RuntimeException("Vendor not found with ID: " + id);
        }

    }

    @Override
    public Page<Vendor> getAllVendors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Vendor> Vendors = vendorRepository.findAll(pageable);
        return Vendors;
    }


}
