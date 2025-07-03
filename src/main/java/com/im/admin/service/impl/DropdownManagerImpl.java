package com.im.admin.service.impl;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.repositories.*;
import com.im.admin.service.DropdownManagerService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DropdownManagerImpl implements DropdownManagerService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private FundRequestRepository fundRequestRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private  PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private  RetailerRepository retailerRepository;


    @Override
    public List<ChildUserDto> getChildUsers(String parentHierarchyId) {
        String parentType = commonMethods.extractHierarchyType(parentHierarchyId); // e.g., DT
        String childEntity = hierarchyChildren.get(parentType); // e.g., retailer

        if (childEntity == null)
            throw new RuntimeException("No child entity found for hierarchy type: " + parentType);

        return switch (childEntity) {
            case "enterprises" -> enterprisesRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                    .map(e -> new ChildUserDto(
                            e.getEnterpriseId(), e.getParentAccount(), e.getAbbreviation(), e.getTitle(),
                            e.getName(), e.getMobileNumber(), e.getEmailId(), e.getDob(), e.getStatus()
                    )).collect(Collectors.toList());

            case "white_labels" -> whiteLabelRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                    .map(w -> new ChildUserDto(
                            w.getWhiteLabelId(), w.getParentAccount(), w.getAbbreviation(), w.getTitle(),
                            w.getName(), w.getMobileNumber(), w.getEmailId(), w.getDob(), w.getStatus()
                    )).collect(Collectors.toList());

            case "super_distributor" -> superDistributorRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                    .map(s -> new ChildUserDto(
                            s.getSuperDistributorId(), s.getParentAccount(), s.getAbbreviation(), s.getTitle(),
                            s.getName(), s.getMobileNumber(), s.getEmailId(), s.getDob(), s.getStatus()
                    )).collect(Collectors.toList());

            case "partner" -> partnerRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                    .map(p -> new ChildUserDto(
                            p.getPartnerId(), p.getParentAccount(), p.getAbbreviation(), p.getTitle(),
                            p.getName(), p.getMobileNumber(), p.getEmailId(), p.getDob(), p.getStatus()
                    )).collect(Collectors.toList());

            case "distributor" -> distributorRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                    .map(d -> new ChildUserDto(
                            d.getDistributorId(), d.getParentAccount(), d.getAbbreviation(), d.getTitle(),
                            d.getName(), d.getMobileNumber(), d.getEmailId(), d.getDob(), d.getStatus()
                    )).collect(Collectors.toList());

            case "retailer" -> retailerRepository.findByParentAccountContaining(parentType)
                    .stream()
                    .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    .map(r -> new ChildUserDto(
                            r.getRetailerId(), r.getParentAccount(), r.getAbbreviation(), r.getTitle(),
                            r.getName(), r.getMobileNumber(), r.getEmailId(), r.getDob(), r.getStatus()
                    )).collect(Collectors.toList());

            default -> throw new RuntimeException("Invalid entity name: " + childEntity);
        };
    }

    private static final Map<String, String> hierarchyChildren = Map.of(
            "SA", "admin",
            "AD", "enterprises",
            "ET", "white_labels",
            "WL", "super_distributor",
            "SD", "partner",
            "PT", "distributor",
            "DT", "retailer"
    );

}
