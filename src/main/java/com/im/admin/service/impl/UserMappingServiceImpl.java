package com.im.admin.service.impl;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.UserMappingService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserMappingServiceImpl implements UserMappingService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private RetailerRepository retailerRepo;

    @Autowired
    private DistributorRepository distributorRepo;

    @Autowired
    private PartnerRepository partnerRepo;

    @Autowired
    private SuperDistributorRepository superDistRepo;

    @Autowired
    private WhiteLabelRepository whitelabelRepo;

    @Autowired
    private EnterprisesRepository enterprisesRepo;

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private SuperAdminRepository superAdminRepo;

    @Override
    public List<?> remapUsers(String fromId, String toId) {
        String type = commonMethods.extractHierarchyType(fromId); // Auto-detect type

        return switch (type) {
            case "RT" -> updateRetailers(fromId, toId);                     // Retailer → (no child, maybe skip)
            case "DT" -> updateDistributors(fromId, toId);                   // Distributor → Retailer
            case "PT" -> updatePartners(fromId, toId);                        // Partner → Distributor
            case "SD" -> updateSuperDistributors(fromId, toId);               // SuperDistributor → Partner
            case "WL" -> updateWhitelabels(fromId, toId);                     // WhiteLabel → SuperDistributor
            case "ET" -> updateEnterprises(fromId, toId);                     // Enterprise → WhiteLabel
            default -> throw new RuntimeException("Invalid hierarchy type from ID: " + fromId);
        };
    }

    /**
     * RT level – usually no child, but keeping for completeness
     */
    private List<Retailer> updateRetailers(String fromId, String toId) {
        List<Retailer> list = retailerRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No retailers found under " + fromId);
        list.forEach(r -> r.setParentAccount(toId));
        return retailerRepo.saveAll(list);
    }

    /**
     * DT → RT mapping
     */
    private List<Retailer> updateDistributors(String fromId, String toId) {
        List<Retailer> list = retailerRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No retailers found under distributor " + fromId);
        list.forEach(r -> r.setParentAccount(toId));
        return retailerRepo.saveAll(list);
    }

    /**
     * PT → DT mapping
     */
    private List<Distributor> updatePartners(String fromId, String toId) {
        List<Distributor> list = distributorRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No distributors found under partner " + fromId);
        list.forEach(d -> d.setParentAccount(toId));
        return distributorRepo.saveAll(list);
    }

    /**
     * SD → PT mapping
     */
    private List<Partner> updateSuperDistributors(String fromId, String toId) {
        List<Partner> list = partnerRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No partners found under super distributor " + fromId);
        list.forEach(p -> p.setParentAccount(toId));
        return partnerRepo.saveAll(list);
    }

    /**
     * WL → SD mapping
     */
    private List<SuperDistributor> updateWhitelabels(String fromId, String toId) {
        List<SuperDistributor> list = superDistRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No super distributors found under whitelabel " + fromId);
        list.forEach(sd -> sd.setParentAccount(toId));
        return superDistRepo.saveAll(list);
    }

    /**
     * ET → WL mapping
     */
    private List<WhiteLabels> updateEnterprises(String fromId, String toId) {
        List<WhiteLabels> list = whitelabelRepo.findByParentAccount(fromId);
        if (list.isEmpty()) throw new RuntimeException("No whitelabels found under enterprise " + fromId);
        list.forEach(wl -> wl.setParentAccount(toId));
        return whitelabelRepo.saveAll(list);
    }
}
