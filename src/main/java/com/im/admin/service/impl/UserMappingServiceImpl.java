package com.im.admin.service.impl;

import com.im.admin.dto.HierarchyUpdateDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.UserMappingService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.UserMappingUpdatePathUtil;
import jakarta.transaction.Transactional;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private UserMappingUpdatePathUtil updateHierarchyMapping;

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
    private List<HierarchyUpdateDTO> updateRetailers(String fromId, String toId) {
        List<Retailer> list = retailerRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No retailers found under " + fromId);
        list.forEach(r -> r.setParentAccount(toId));
        List<Retailer> retailers = retailerRepo.saveAll(list);

        return retailers.stream()
                .map(r -> new HierarchyUpdateDTO(
                        r.getRetailerId(),    // id
                        r.getParentAccount(), // parentAccount
                        r.getName(),          // name
                        r.getMobileNumber()   // mobileNumber
                ))
                .toList();

    }

    /**
     * DT → RT mapping
     */
    private List<HierarchyUpdateDTO> updateDistributors(String fromId, String toId) {
        List<Retailer> list = retailerRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No retailers found under distributor " + fromId);
        list.forEach(r -> r.setParentAccount(toId));
        List<Retailer> saved  = retailerRepo.saveAll(list);

        return saved.stream()
                .map(r -> new HierarchyUpdateDTO(
                        r.getRetailerId(),    // id
                        r.getParentAccount(), // parentAccount
                        r.getName(),          // name
                        r.getMobileNumber()   // mobileNumber
                ))
                .toList();
    }

    /**
     * PT → DT mapping
     */
    private List<HierarchyUpdateDTO> updatePartners(String fromId, String toId) {
        List<Distributor> list = distributorRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No distributors found under partner " + fromId);
        list.forEach(d -> d.setParentAccount(toId));
        List<Distributor> saveddistributors = distributorRepo.saveAll(list);

        return saveddistributors.stream()
                .map(d -> new HierarchyUpdateDTO(
                        d.getDistributorId(),
                        d.getParentAccount(),
                        d.getName(),
                        d.getMobileNumber()
                ))
                .toList();

    }

    /**
     * SD → PT mapping
     */
    private List<HierarchyUpdateDTO> updateSuperDistributors(String fromId, String toId) {
        List<Partner> list = partnerRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No partners found under super distributor " + fromId);
        list.forEach(p -> p.setParentAccount(toId));
        List<Partner> savedpartners = partnerRepo.saveAll(list);

        return savedpartners.stream()
                .map(p -> new HierarchyUpdateDTO(
                        p.getPartnerId(),
                        p.getParentAccount(),
                        p.getName(),
                        p.getMobileNumber()
                ))
                .toList();

    }

    /**
     * WL → SD mapping
     */
    private List<HierarchyUpdateDTO> updateWhitelabels(String fromId, String toId) {
        List<SuperDistributor> list = superDistRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No super distributors found under whitelabel " + fromId);
        list.forEach(sd -> sd.setParentAccount(toId));
        List<SuperDistributor> savedsuperDistributors = superDistRepo.saveAll(list);

        return savedsuperDistributors.stream()
                .map(s -> new HierarchyUpdateDTO(
                        s.getSuperDistributorId(),
                        s.getParentAccount(),
                        s.getName(),
                        s.getMobileNumber()
                ))
                .toList();

    }

    /**
     * ET → WL mapping
     */
    private List<HierarchyUpdateDTO> updateEnterprises(String fromId, String toId) {
        List<WhiteLabels> list = whitelabelRepo.findByParentAccountContainingIgnoreCase(fromId);
        if (list.isEmpty()) throw new RuntimeException("No whitelabels found under enterprise " + fromId);
        list.forEach(wl -> wl.setParentAccount(toId));
        List<WhiteLabels> savedwhiteLabels = whitelabelRepo.saveAll(list);

        return savedwhiteLabels.stream()
                .map(w -> new HierarchyUpdateDTO(
                        w.getWhiteLabelId(),
                        w.getParentAccount(),
                        w.getName(),
                        w.getMobileNumber()
                ))
                .toList();
    }


    @Transactional
    @Override
    public List<HierarchyUpdateDTO> remapMultipleUsers(List<String> userIds, String newParentId) {
        List<HierarchyUpdateDTO> result = new ArrayList<>();

        for (String userId : userIds) {
            String type = commonMethods.extractHierarchyType(userId);

            HierarchyUpdateDTO updated = switch (type) {
                case "RT" -> updateSingleRetailer(userId, newParentId);
                case "DT" -> updateSingleDistributor(userId, newParentId);
                case "PT" -> updateSinglePartner(userId, newParentId);
                case "SD" -> updateSingleSuperDistributor(userId, newParentId);
                case "WL" -> updateSingleWhiteLabel(userId, newParentId);
                case "ET" -> updateSingleEnterprise(userId, newParentId);
                default -> throw new RuntimeException("Invalid hierarchy type for ID: " + userId);
            };

            result.add(updated);
        }

        return result;
    }


    /**
     * RT → change parent distributor
     */
    private HierarchyUpdateDTO updateSingleRetailer(String retailerId, String newParentId) {
        Retailer retailer = retailerRepo.findById(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + retailerId));
        retailer.setParentAccount(newParentId);
        Retailer saved = retailerRepo.save(retailer);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(retailerId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                retailerId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getRetailerId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }

    /**
     * DT → change parent partner
     */
    private HierarchyUpdateDTO updateSingleDistributor(String distributorId, String newParentId) {
        Distributor distributor = distributorRepo.findById(distributorId)
                .orElseThrow(() -> new RuntimeException("Distributor not found with ID: " + distributorId));
        distributor.setParentAccount(newParentId);
        Distributor saved = distributorRepo.save(distributor);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(distributorId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                distributorId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getDistributorId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }

    /**
     * PT → change parent super distributor
     */
    private HierarchyUpdateDTO updateSinglePartner(String partnerId, String newParentId) {
        Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with ID: " + partnerId));
        partner.setParentAccount(newParentId);
        Partner saved = partnerRepo.save(partner);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(partnerId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                partnerId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getPartnerId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }

    /**
     * SD → change parent whitelabel
     */
    private HierarchyUpdateDTO updateSingleSuperDistributor(String sdId, String newParentId) {
        SuperDistributor sd = superDistRepo.findById(sdId)
                .orElseThrow(() -> new RuntimeException("Super Distributor not found with ID: " + sdId));
        sd.setParentAccount(newParentId);
        SuperDistributor saved = superDistRepo.save(sd);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(sdId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                sdId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getSuperDistributorId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }

    /**
     * WL → change parent enterprise
     */
    private HierarchyUpdateDTO updateSingleWhiteLabel(String wlId, String newParentId) {
        WhiteLabels wl = whitelabelRepo.findById(wlId)
                .orElseThrow(() -> new RuntimeException("WhiteLabel not found with ID: " + wlId));
        wl.setParentAccount(newParentId);
        WhiteLabels saved = whitelabelRepo.save(wl);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(wlId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                wlId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getWhiteLabelId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }

    /**
     * ET → (Top level, maybe admin change)
     */
    private HierarchyUpdateDTO updateSingleEnterprise(String enterpriseId, String newParentId) {
        Enterprises et = enterprisesRepo.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));
        et.setParentAccount(newParentId);
        Enterprises saved = enterprisesRepo.save(et);

        Optional<UserDetails> optUserDetails = userDetailsRepository.findByHierarchyId(enterpriseId);
        UserDetails userDetails = optUserDetails.get();
        userDetails.setParentAccount(newParentId);
        userDetailsRepository.save(userDetails);

        updateHierarchyMapping.updateHierarchyMapping(
                enterpriseId,
                newParentId
        );

        return new HierarchyUpdateDTO(saved.getEnterpriseId(), saved.getParentAccount(), saved.getName(), saved.getMobileNumber());
    }


    @Override
    public List<HierarchyUpdateDTO> getUsersByType(String userType) {
        return switch (userType) {
            case "RT" -> retailerRepo.findAll()
                    .stream()
                    .map(r -> new HierarchyUpdateDTO(r.getRetailerId(), r.getParentAccount(), r.getName(), r.getMobileNumber()))
                    .toList();

            case "DT" -> distributorRepo.findAll()
                    .stream()
                    .map(d -> new HierarchyUpdateDTO(d.getDistributorId(), d.getParentAccount(), d.getName(), d.getMobileNumber()))
                    .toList();

            case "PT" -> partnerRepo.findAll()
                    .stream()
                    .map(p -> new HierarchyUpdateDTO(p.getPartnerId(), p.getParentAccount(), p.getName(), p.getMobileNumber()))
                    .toList();

            case "SD" -> superDistRepo.findAll()
                    .stream()
                    .map(sd -> new HierarchyUpdateDTO(sd.getSuperDistributorId(), sd.getParentAccount(), sd.getName(), sd.getMobileNumber()))
                    .toList();

            case "WL" -> whitelabelRepo.findAll()
                    .stream()
                    .map(wl -> new HierarchyUpdateDTO(wl.getWhiteLabelId(), wl.getParentAccount(), wl.getName(), wl.getMobileNumber()))
                    .toList();

            case "ET" -> enterprisesRepo.findAll()
                    .stream()
                    .map(et -> new HierarchyUpdateDTO(et.getEnterpriseId(), et.getParentAccount(), et.getName(), et.getMobileNumber()))
                    .toList();

            default -> throw new RuntimeException("Invalid userType: " + userType);
        };
    }


}
