package com.im.admin.service.impl;

import com.im.admin.entities.SuperDistributor;
import com.im.admin.entities.SuperDistributorPostpaidAmount;
import com.im.admin.entities.SuperDistributorPrepaidAmount;
import com.im.admin.entities.WhiteLabels;
import com.im.admin.repositories.SuperDistributorPostpaidAmountRepository;
import com.im.admin.repositories.SuperDistributorPrepaidAmountRepository;
import com.im.admin.repositories.SuperDistributorRepository;
import com.im.admin.service.SuperDistributorService;
import com.im.admin.util.IdGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperDistributorServiceImpl implements SuperDistributorService {

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    @Transactional
    public SuperDistributor saveSuperDistributor(SuperDistributor superDistributor) {
        try {

            String abbreviation = superDistributor.getAbbreviation();

            superDistributor.setSuperDistributorId(idGenerator.generateHierarchyEntityId("Super Distributor", abbreviation));
            SuperDistributor savedDistributor = superDistributorRepository.save(superDistributor);

            SuperDistributorPrepaidAmount prepaidAmount = new SuperDistributorPrepaidAmount();
            prepaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPrepaidAmountRepository.save(prepaidAmount);
            log.info("Prepaid amount record created for Super Distributor: {}", superDistributor.getSuperDistributorId());

            SuperDistributorPostpaidAmount postpaidAmount = new SuperDistributorPostpaidAmount();
            postpaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPostpaidAmountRepository.save(postpaidAmount);
            log.info("Postpaid amount record created for Super Distributor: {}", superDistributor.getSuperDistributorId());

            return savedDistributor;
        } catch (Exception e) {
            e.printStackTrace(); // Print detailed error logs
            throw new RuntimeException("Failed to save SuperDistributor and related data", e);
        }
    }


    @Override
    public SuperDistributor getSuperDistributor(String id) {
        return superDistributorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperDistributor not found with id: " + id));
    }

    @Override
    public SuperDistributor updateSuperDistributor(String id, SuperDistributor updatedDistributor) {
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
        return superDistributorRepository.save(existingDistributor);
    }


    @Override
    public List<SuperDistributor> getAllSuperDistributors() {
        return superDistributorRepository.findAll();
    }

    @Transactional
    @Override
    public void deleteSuperDistributor(String id) {
        log.info("Deleting Super Distributor: {}", id);

        SuperDistributor superDistributor = superDistributorRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("SuperDistributor not found with ID: " + id));

        superDistributorPrepaidAmountRepository.deleteBySuperDistributorId(superDistributor);
        superDistributorPostpaidAmountRepository.deleteBySuperDistributorId(superDistributor);
        superDistributorRepository.delete(superDistributor);
        log.info("SuperDistributor deleted successfully: {}", id);

    }
}

