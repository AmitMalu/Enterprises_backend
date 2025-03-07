package com.im.admin.service.impl;

import com.im.admin.entities.vendor.Vendors;
import com.im.admin.exception.ResourceNotFoundException;
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


@Service
@Slf4j
public class VendorServiceImpl implements VendorService {

    @Autowired
    private VendorRepository vendorsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public Vendors saveVendor(Vendors vendor) {
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

        existingVendor.setInfo(updatedVendor.getInfo());
        existingVendor.setVendorAccount(updatedVendor.getVendorAccount());
        existingVendor.setEditedOn(LocalDateTime.now());

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
}

