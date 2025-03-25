package com.im.admin.service;

import com.im.admin.entities.SuperDistributor;

import java.util.List;

public interface SuperDistributorService {

    SuperDistributor saveSuperDistributor(SuperDistributor distributor);

    SuperDistributor getSuperDistributor(String id);

    SuperDistributor updateSuperDistributor(String id, SuperDistributor distributor);

    List<SuperDistributor> getAllSuperDistributors();

    void deleteSuperDistributor(String id);
}

