package com.im.admin.service.impl;

import com.im.admin.dto.RetailerDTO;
import com.im.admin.entities.Retailer;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.service.RetailerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RetailerServiceImpl implements RetailerService {

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    public RetailerServiceImpl(RetailerRepository retailerRepository) {
        this.retailerRepository = retailerRepository;
    }

    @Override
    public RetailerDTO saveRetailer(RetailerDTO retailerDTO) {
        phoneNumberValidationService.validatePhoneNumber(retailerDTO.getInfo().getMobile());

        Retailer retailer = mapToEntity(retailerDTO);
        Retailer savedRetailer = retailerRepository.save(retailer);
        return mapToDTO(savedRetailer);
    }

    @Override
    public RetailerDTO updateRetailer(Long id, RetailerDTO retailerDTO) {
        Retailer existingRetailer = retailerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + id));

        existingRetailer.setParentAccount(retailerDTO.getParentAccount());
        existingRetailer.setInfo(retailerDTO.getInfo());
        existingRetailer.setMccCategory(retailerDTO.getMccCategory());
        existingRetailer.setOrmsId(retailerDTO.getOrmsId());
        existingRetailer.setCompetitor(retailerDTO.getCompetitor());
        existingRetailer.setAddress(retailerDTO.getAddress());
        existingRetailer.setOutletAddress(retailerDTO.getOutletAddress());
        existingRetailer.setDistrict(retailerDTO.getDistrict());

        return mapToDTO(retailerRepository.save(existingRetailer));
    }

    @Override
    public List<RetailerDTO> getAllRetailers() {
        return retailerRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RetailerDTO getRetailerById(Long id) {
        Retailer retailer = retailerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + id));
        return mapToDTO(retailer);
    }

    @Override
    public void deleteRetailer(Long id) {
        Retailer retailer = retailerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + id));
        retailerRepository.delete(retailer);
    }

    private RetailerDTO mapToDTO(Retailer retailer) {
        return new RetailerDTO(
                retailer.getId(),
                retailer.getParentAccount(),
                retailer.getInfo(),
                retailer.getMccCategory(),
                retailer.getOrmsId(),
                retailer.getCompetitor(),
                retailer.getAddress(),
                retailer.getOutletAddress(),
                retailer.getDistrict()
        );
    }

    private Retailer mapToEntity(RetailerDTO retailerDTO) {
        Retailer retailer = new Retailer();
        retailer.setId(retailerDTO.getId());
        retailer.setParentAccount(retailerDTO.getParentAccount());
        retailer.setInfo(retailerDTO.getInfo());
        retailer.setMccCategory(retailerDTO.getMccCategory());
        retailer.setOrmsId(retailerDTO.getOrmsId());
        retailer.setCompetitor(retailerDTO.getCompetitor());
        retailer.setAddress(retailerDTO.getAddress());
        retailer.setOutletAddress(retailerDTO.getOutletAddress());
        retailer.setDistrict(retailerDTO.getDistrict());
        return retailer;
    }
}

