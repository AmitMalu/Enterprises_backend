package com.im.admin.service;


import com.im.admin.dto.VendorActiveResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VendorService {

    Vendors saveVendor(Vendors vendor);

    Vendors getVendorById(String id);

    Page<Vendors> getAllVendors(Pageable pageable, List<String> id, String vendorName,
                                String vendorCode, String productId,
                                String productCode, String subProductId,
                                String subProductCode, String modesSupported,
                                Integer priority, Boolean isActive,
                                String bankType, String website,
                                String logo, String parameter,
                                String createdBy, String editedBy,
                                LocalDate createdOn,
                                LocalDate editedOn);


    Vendors updateVendor(String id, Vendors vendor);

    void deleteVendor(String id);

    List<VendorDropdownResponseDto> getActiveVendorByProductId(String productId);

    List<VendorActiveResponseDto> getAllActiveVendors();
}

