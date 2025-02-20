package com.im.admin.service.impl;

import com.im.admin.entities.SuperDistributor;
import com.im.admin.repositories.SuperDistributorRepository;
import com.im.admin.service.SuperDistributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperDistributorServiceImpl implements SuperDistributorService {

    @Autowired
    private SuperDistributorRepository repository;

    @Override
    public SuperDistributor saveSuperDistributor(SuperDistributor distributor) {
        return repository.save(distributor);
    }

    @Override
    public SuperDistributor getSuperDistributor(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperDistributor not found with id: " + id));
    }

    @Override
    public SuperDistributor updateSuperDistributor(Long id, SuperDistributor updatedDistributor) {
        SuperDistributor existingDistributor = getSuperDistributor(id);
        existingDistributor.setInfo(updatedDistributor.getInfo());
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

