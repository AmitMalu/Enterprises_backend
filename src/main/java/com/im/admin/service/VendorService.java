package com.im.admin.service;


import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorService {

    Vendors saveVendor(Vendors vendor);

    Vendors getVendorById(String id);

    Page<Vendors> getAllVendors(int page, int size);

    Vendors updateVendor(String id, Vendors vendor);

    void deleteVendor(String id);

    List<VendorDropdownResponseDto> getActiveVendorBySubProductId(String subProductId);
}

