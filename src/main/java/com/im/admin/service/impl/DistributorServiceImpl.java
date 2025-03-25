package com.im.admin.service.impl;

import com.im.admin.dto.DistributorDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPostpaidAmount;
import com.im.admin.entities.DistributorPrepaidAmount;
import com.im.admin.repositories.DistributorPostpaidAmountRepository;
import com.im.admin.repositories.DistributorPrepaidAmountRepository;
import com.im.admin.repositories.DistributorRepository;
import com.im.admin.service.DistributorService;
import com.im.admin.util.IdGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributorServiceImpl implements DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private DistributorPrepaidAmountRepository prepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository postpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Transactional
    @Override
    public Distributor createDistributor(Distributor distributor) {

        distributor.setDistributorId(idGenerator.generateHierarchyEntityId("Distributor", distributor.getAbbreviation()));

        Distributor savedDistributor = distributorRepository.save(distributor);

        DistributorPrepaidAmount prepaid = new DistributorPrepaidAmount();
        prepaid.setDistributorId(savedDistributor);
        prepaidAmountRepository.save(prepaid);

        DistributorPostpaidAmount postpaid = new DistributorPostpaidAmount();
        postpaid.setDistributorId(savedDistributor);
        postpaidAmountRepository.save(postpaid);

        return savedDistributor;
    }

    @Transactional
    @Override
    public Distributor updateDistributor(String id, Distributor distributor) {
        Distributor existingDistributor = distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        existingDistributor.setTitle(distributor.getTitle());
        existingDistributor.setName(distributor.getName());
        existingDistributor.setMobile(distributor.getMobile());
        existingDistributor.setAbbreviation(distributor.getAbbreviation());
        existingDistributor.setEmail(distributor.getEmail());
        existingDistributor.setDob(distributor.getDob());
        existingDistributor.setOutletName(distributor.getOutletName());
        existingDistributor.setCompanyType(distributor.getCompanyType());
        existingDistributor.setCompetitor(distributor.getCompetitor());
        existingDistributor.setAddress(distributor.getAddress());
        existingDistributor.setOutletAddress(distributor.getOutletAddress());

        return distributorRepository.save(existingDistributor);
    }

    // ✅ Get All Distributors
    @Override
    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    @Override
    public Distributor getDistributorById(String id) {
        return distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
    }

    @Transactional
    @Override
    public void deleteDistributor(String id) {
        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        prepaidAmountRepository.deleteByDistributorId(distributor);
        postpaidAmountRepository.deleteByDistributorId(distributor);
        distributorRepository.delete(distributor);
    }

}
