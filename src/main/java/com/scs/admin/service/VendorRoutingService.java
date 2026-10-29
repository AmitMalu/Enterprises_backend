package com.scs.admin.service;

import com.scs.admin.dto.VendorRoutingRequestDTO;
import com.scs.admin.dto.VendorRoutingResponseDTO;
import com.scs.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface VendorRoutingService {

    CommonResponse<VendorRoutingResponseDTO> save(VendorRoutingRequestDTO request);

    CommonResponse<VendorRoutingResponseDTO> update(Long id, VendorRoutingRequestDTO request);

    CommonResponse<VendorRoutingResponseDTO> getById(Long id);

    CommonResponse<Page<VendorRoutingResponseDTO>> getAll(Map<String, String> filters, Pageable pageable);
}

