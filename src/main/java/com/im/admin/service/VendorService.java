package com.im.admin.service;

import com.im.admin.entities.Vendor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.util.Optional;

public interface VendorService {
    Vendor saveVendor(@Valid Vendor vendor);

    void deleteVendor(Long id);

    Optional<Vendor> getVendorById(Long id);

    Vendor updateVendor(Long id, @Valid Vendor vendor);

    Page<Vendor> getAllVendors(int page, int size);
}
