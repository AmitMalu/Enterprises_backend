package com.im.admin.service;


import com.im.admin.entities.Distributor;

import java.util.List;

public interface DistributorService {

    Distributor createDistributor(Distributor distributor);

    Distributor updateDistributor(String id, Distributor distributor);

    List<Distributor> getAllDistributors();

    Distributor getDistributorById(String id);

    void deleteDistributor(String id);
}


