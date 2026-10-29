package com.scs.admin.service;


import com.scs.admin.dto.VendorActiveResponseDto;
import com.scs.admin.dto.VendorDropdownResponseDto;
import com.scs.admin.dto.VendorsDTO;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VendorService {

    Vendors saveVendor(VendorsDTO dto);

    VendorsDTO getVendorById(String id);

    Page<VendorsDTO> getAllVendors(Pageable pageable,
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
                                   LocalDate editedOn);


    Vendors updateVendor(String id, VendorsDTO dto);

    void deleteVendor(String id);

    List<VendorDropdownResponseDto> getActiveVendorByProductId(Short productId);

    List<VendorActiveResponseDto> getAllActiveVendors();

    CommonResponse<List<VendorDropdownResponseDto>> getVendorsBySubProduct(String subProductId);

}

