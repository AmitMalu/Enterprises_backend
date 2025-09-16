package com.im.admin.service;

import com.im.admin.dto.VendorRoutingRequestDTO;
import com.im.admin.dto.VendorRoutingResponseDTO;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface VendorRoutingService {

    CommonResponse<VendorRoutingResponseDTO> save(VendorRoutingRequestDTO request);

    CommonResponse<VendorRoutingResponseDTO> update(Long id, VendorRoutingRequestDTO request);

    CommonResponse<VendorRoutingResponseDTO> getById(Long id);

    CommonResponse<Page<VendorRoutingResponseDTO>> getAll(Map<String, String> filters, Pageable pageable);
}

