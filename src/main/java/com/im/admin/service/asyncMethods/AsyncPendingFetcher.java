package com.im.admin.service.asyncMethods;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncPendingFetcher {

    @Async
    public CompletableFuture<List<Retailer>> fetchRetailers(String status) {
        return CompletableFuture.completedFuture(retailerRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<Distributor>> fetchDistributors(String status) {
        return CompletableFuture.completedFuture(distributorRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<Partner>> fetchPartners(String status) {
        return CompletableFuture.completedFuture(partnerRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<SuperDistributor>> fetchSuperDistributors(String status) {
        return CompletableFuture.completedFuture(superDistributorRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<WhiteLabels>> fetchWhiteLabels(String status) {
        return CompletableFuture.completedFuture(whiteLabelsRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<Enterprises>> fetchEnterprises(String status) {
        return CompletableFuture.completedFuture(enterprisesRepository.findByStatus(status));
    }

    @Async
    public CompletableFuture<List<Admin>> fetchAdmins(String status) {
        return CompletableFuture.completedFuture(adminRepository.findByStatus(status));
    }

    @Autowired
    private RetailerRepository retailerRepository;
    @Autowired private DistributorRepository distributorRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private SuperDistributorRepository superDistributorRepository;
    @Autowired private WhiteLabelRepository whiteLabelsRepository;
    @Autowired private EnterprisesRepository enterprisesRepository;
    @Autowired private AdminRepository adminRepository;
}

