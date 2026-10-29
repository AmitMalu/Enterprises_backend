package com.scs.admin.service;

import com.scs.admin.dto.VendorCredentialsDTO;
import com.scs.admin.dto.VendorCredentialsResponseDTO;
import com.scs.admin.entities.VendorCredentials;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface VendorCredentialsService {

    VendorCredentials saveVendorCredentials(VendorCredentialsDTO dto);

    VendorCredentialsResponseDTO updateVendorCredentials(Long id, VendorCredentialsDTO dto);

    Page<VendorCredentialsResponseDTO> getAllVendorCredentials(
            String productName,
            String vendorName,
            String whiteLabelAdminName,
            String username,
            Boolean isActive,
            String createdBy,
            String editedBy,
            LocalDate createdOn,
            LocalDate editedOn,
            Pageable pageable);

    VendorCredentialsResponseDTO getVendorCredentialsById(Long id);
}

