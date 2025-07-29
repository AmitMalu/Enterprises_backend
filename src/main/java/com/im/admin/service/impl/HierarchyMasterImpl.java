package com.im.admin.service.impl;

import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;
import com.im.admin.entities.HierarchyMaster;
import com.im.admin.repositories.*;
import com.im.admin.service.HierarchyMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HierarchyMasterImpl implements HierarchyMasterService {

    @Autowired
    private HierarchyMasterRepository hierarchyMasterRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

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
    public List<HierarchyMasterDto> getAllNameAndCode() {
        List<HierarchyMaster> all = hierarchyMasterRepository.findAll();
        return all.stream()
                .map(h -> new HierarchyMasterDto(h.getName(), h.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public List<HierarchyResponseDto> getHierarchyDetails(String hierarchyCode) {
        // Step 1: Fetch hierarchy_ids for given code
        List<String> hierarchyIds = userDetailsRepository.findHierarchyIdsByHierarchyCode(hierarchyCode);

        List<HierarchyResponseDto> result = new ArrayList<>();

        for (String hierarchyId : hierarchyIds) {
            String[] parts = hierarchyId.split("-"); // e.g., IM-AD-001
            String code = parts[1]; // AD

            String name = null;
            switch (code) {
                case "AD" -> name = adminRepository.findNameByHierarchyId(hierarchyId);
                case "RT" -> name = retailerRepository.findNameByHierarchyId(hierarchyId);
                case "PT" -> name = partnerRepository.findNameByHierarchyId(hierarchyId);
                case "DT" -> name = distributorRepository.findNameByHierarchyId(hierarchyId);
                case "SD" -> name = superDistributorRepository.findNameByHierarchyId(hierarchyId);
                case "WL" -> name = whiteLabelRepository.findNameByHierarchyId(hierarchyId);
                case "ET" -> name = enterprisesRepository.findNameByHierarchyId(hierarchyId);
//                case "SA" -> name = superAdminRepository.findNameByHierarchyId(hierarchyId);
            }

            if (name != null) {
                HierarchyResponseDto dto = new HierarchyResponseDto();
                dto.setId(hierarchyId);
                dto.setDisplayName(name + "#" + hierarchyId);
                result.add(dto);
            }
        }

        return result;
    }

}
