package com.im.admin.service.impl;

import com.im.admin.entities.SuperDistributor;
import com.im.admin.entities.SuperDistributorPostpaidAmount;
import com.im.admin.entities.SuperDistributorPrepaidAmount;
import com.im.admin.repositories.SuperDistributorPostpaidAmountRepository;
import com.im.admin.repositories.SuperDistributorPrepaidAmountRepository;
import com.im.admin.repositories.SuperDistributorRepository;
import com.im.admin.service.SuperDistributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperDistributorServiceImpl implements SuperDistributorService {

    @Autowired
    private SuperDistributorRepository repository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Override
    @Transactional
    public SuperDistributor saveSuperDistributor(SuperDistributor distributor) {
        try {
            phoneNumberValidationService.validatePhoneNumber(distributor.getMobile());

            SuperDistributor savedDistributor = repository.save(distributor);

            superDistributorPrepaidAmountRepository.save(new SuperDistributorPrepaidAmount());
            superDistributorPostpaidAmountRepository.save(new SuperDistributorPostpaidAmount());

            return savedDistributor;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save SuperDistributor and related data", e);
        }
    }


    @Override
    public SuperDistributor getSuperDistributor(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperDistributor not found with id: " + id));
    }

    @Override
    public SuperDistributor updateSuperDistributor(Long id, SuperDistributor updatedDistributor) {
        SuperDistributor existingDistributor = getSuperDistributor(id);
        existingDistributor.setTitle(updatedDistributor.getTitle());
        existingDistributor.setName(updatedDistributor.getName());
        existingDistributor.setMobile(updatedDistributor.getMobile());
        existingDistributor.setEmail(updatedDistributor.getEmail());
        existingDistributor.setDob(updatedDistributor.getDob());
        existingDistributor.setOutletName(updatedDistributor.getOutletName());
        existingDistributor.setProductGroups(updatedDistributor.getProductGroups());
        existingDistributor.setCompanyType(updatedDistributor.getCompanyType());
        existingDistributor.setAddress(updatedDistributor.getAddress());
        existingDistributor.setOutletAddress(updatedDistributor.getOutletAddress());
        return repository.save(existingDistributor);
    }

    @Override
    public List<SuperDistributor> getAllSuperDistributors() {
        return repository.findAll();
    }

    @Override
    public void deleteSuperDistributor(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new RuntimeException("SuperDistributor not found with id: " + id);
        }
    }
}

