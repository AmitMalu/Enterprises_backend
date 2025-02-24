package com.im.admin.service;

import com.im.admin.dto.DistributorDTO;
import org.springframework.data.domain.Page;

public interface DistributorService {

    DistributorDTO saveDistributor(DistributorDTO distributorDTO);

    DistributorDTO updateDistributor(Long id, DistributorDTO distributorDTO);

    DistributorDTO getDistributorById(Long id);

    Page<DistributorDTO> getAllDistributors(int page, int size);
}

