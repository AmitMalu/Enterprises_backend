package com.im.admin.service;


import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface VendorService {

    Vendors saveVendor(Vendors vendor);

    Vendors updateVendor(Long id, Vendors vendor);

    Page<Vendors> getAllVendors(Pageable pageable);

    Vendors getVendorById(Long id);

    void deleteVendor(Long id);
}

