package com.im.admin.service;

import com.im.admin.entities.SuperDistributor;

import java.util.List;

public interface SuperDistributorService {

    SuperDistributor saveSuperDistributor(SuperDistributor distributor);

    SuperDistributor getSuperDistributor(Long id);

    SuperDistributor updateSuperDistributor(Long id, SuperDistributor distributor);

    List<SuperDistributor> getAllSuperDistributors();

    void deleteSuperDistributor(Long id);
}

