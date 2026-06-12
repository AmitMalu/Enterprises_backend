package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.HierarchyMasterService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;


    @Override
    public List<HierarchyMasterDto> getAllNameAndCode() {
        List<HierarchyMaster> all = hierarchyMasterRepository.findAll();
        return all.stream()
                .map(h -> new HierarchyMasterDto(h.getName(), h.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public List<HierarchyResponseDto> getHierarchyDetails(String hierarchyCode) {

        String loginHierarchyId = tokenUtil.extractHierarchyId();
        String loginType = commonMethods.extractHierarchyType(loginHierarchyId);

        List<String> hierarchyIds;

        // Admin can see all records
        if ("AD".equalsIgnoreCase(loginType)) {

            hierarchyIds =
                    userDetailsRepository.findHierarchyIdsByHierarchyCode(
                            hierarchyCode);

        } else {

            hierarchyIds =
                    accessHierarchyPathRepository
                            .findDescendantsByAncestorAndType(
                                    loginHierarchyId,
                                    hierarchyCode
                            );
        }

        List<HierarchyResponseDto> result = new ArrayList<>();

        for (String hierarchyId : hierarchyIds) {

            String type =
                    commonMethods.extractHierarchyType(hierarchyId);

            switch (type) {

                case "RT" -> {
                    Retailer rt = retailerRepository.findById(hierarchyId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Retailer not found: " + hierarchyId));

                    result.add(
                            buildDto(
                                    rt.getRetailerId(),
                                    rt.getName(),
                                    rt.getMobileNumber(),
                                    rt.getEmailId()
                            )
                    );
                }

                case "AD" -> {
                    Admin ad = adminRepository.findById(hierarchyId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Admin not found: " + hierarchyId));

                    result.add(
                            buildDto(
                                    ad.getId(),
                                    ad.getName(),
                                    ad.getMobileNumber(),
                                    ad.getEmailId()
                            )
                    );
                }

                case "DT" -> {
                    Distributor dt = distributorRepository.findById(hierarchyId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Distributor not found: " + hierarchyId));

                    result.add(
                            buildDto(
                                    dt.getDistributorId(),
                                    dt.getName(),
                                    dt.getMobileNumber(),
                                    dt.getEmailId()
                            )
                    );
                }

                case "PT" -> {
                    Partner pt = partnerRepository.findById(hierarchyId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Partner not found: " + hierarchyId));

                    result.add(
                            buildDto(
                                    pt.getPartnerId(),
                                    pt.getName(),
                                    pt.getMobileNumber(),
                                    pt.getEmailId()
                            )
                    );
                }

                case "SD" -> {
                    SuperDistributor sd =
                            superDistributorRepository.findById(hierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Super Distributor not found: "
                                                            + hierarchyId));

                    result.add(
                            buildDto(
                                    sd.getSuperDistributorId(),
                                    sd.getName(),
                                    sd.getMobileNumber(),
                                    sd.getEmailId()
                            )
                    );
                }

                case "WL" -> {
                    WhiteLabels wl =
                            whiteLabelRepository.findById(hierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "WhiteLabel not found: "
                                                            + hierarchyId));

                    result.add(
                            buildDto(
                                    wl.getWhiteLabelId(),
                                    wl.getName(),
                                    wl.getMobileNumber(),
                                    wl.getEmailId()
                            )
                    );
                }

                case "ET" -> {
                    Enterprises et =
                            enterprisesRepository.findById(hierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Enterprise not found: "
                                                            + hierarchyId));

                    result.add(
                            buildDto(
                                    et.getEnterpriseId(),
                                    et.getName(),
                                    et.getMobileNumber(),
                                    et.getEmailId()
                            )
                    );
                }
            }
        }

        return result;
    }

    private HierarchyResponseDto buildDto(String hierarchyId, String name, String mobileNo,String email) {
        HierarchyResponseDto dto = new HierarchyResponseDto();
        dto.setId(hierarchyId);
        dto.setDisplayName(name + "#" + hierarchyId);
        dto.setName(name);
        dto.setMobileno(mobileNo);
        dto.setEmail(email);
        dto.setPrepaidAmount(commonMethods.getCurrentBalance(hierarchyId, "PREPAID"));
        dto.setPostpaidAmount(commonMethods.getCurrentBalance(hierarchyId, "POSTPAID"));
        return dto;
    }



}
