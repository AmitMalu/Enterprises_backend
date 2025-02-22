package com.im.admin.service.impl;

import com.im.admin.dto.DistributorDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.repositories.DistributorRepository;
import com.im.admin.service.DistributorService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DistributorServiceImpl implements DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public DistributorDTO saveDistributor(DistributorDTO distributorDTO) {
        Distributor distributor = modelMapper.map(distributorDTO, Distributor.class);
        Distributor savedDistributor = distributorRepository.save(distributor);
        return modelMapper.map(savedDistributor, DistributorDTO.class);
    }

    @Override
    public DistributorDTO updateDistributor(Long id, DistributorDTO distributorDTO) {
        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Distributor not found"));

        modelMapper.map(distributorDTO, distributor); // Update only changed fields
        Distributor updatedDistributor = distributorRepository.save(distributor);
        return modelMapper.map(updatedDistributor, DistributorDTO.class);
    }

    @Override
    public DistributorDTO getDistributorById(Long id) {
        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Distributor not found"));
        return modelMapper.map(distributor, DistributorDTO.class);
    }

    @Override
    public Page<DistributorDTO> getAllDistributors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Distributor> distributors = distributorRepository.findAll(pageable);
        return distributors.map(distributor -> modelMapper.map(distributor, DistributorDTO.class));
    }
}

