package com.im.admin.service;

import com.im.admin.dto.RetailerDTO;

import java.util.List;

public interface RetailerService {

    RetailerDTO saveRetailer(RetailerDTO retailerDTO);

    RetailerDTO updateRetailer(Long id, RetailerDTO retailerDTO);

    List<RetailerDTO> getAllRetailers();

    RetailerDTO getRetailerById(Long id);

    void deleteRetailer(Long id);
}

