package com.im.admin.service;

import com.im.admin.dto.VendorCredentialsDTO;
import com.im.admin.dto.VendorCredentialsResponseDTO;
import com.im.admin.entities.VendorCredentials;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorCredentialsService {

    VendorCredentials saveVendorCredentials(VendorCredentialsDTO dto);

    VendorCredentials updateVendorCredentials(Long id, VendorCredentialsDTO dto);

    Page<VendorCredentialsResponseDTO> getAllVendorCredentials(
            String productName,
            String vendorName,
            String whiteLabelAdminName,
            String username,
            Boolean isActive,
            String createdBy,
            Pageable pageable);

    VendorCredentialsResponseDTO getVendorCredentialsById(Long id);
}

