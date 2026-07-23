package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.HierarchyMasterService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
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

    @Autowired
    private HierarchyDataService hierarchyDataService;


    @Override
    public List<HierarchyMasterDto> getAllNameAndCode() {
        List<HierarchyMaster> all = hierarchyMasterRepository.findAll();
        return all.stream()
                .map(h -> new HierarchyMasterDto(h.getName(), h.getCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HierarchyResponseDto> getHierarchyDetails(
            String hierarchyCode
    ) {

        if (hierarchyCode == null || hierarchyCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Hierarchy code is required"
            );
        }

        final String requestedHierarchyCode =
                hierarchyCode.trim().toUpperCase();

        final String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalStateException(
                    "Login hierarchy ID is not available"
            );
        }

        final String loginHierarchyType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        if (loginHierarchyType == null
                || loginHierarchyType.isBlank()) {

            throw new IllegalStateException(
                    "Unable to identify login hierarchy type: "
                            + loginHierarchyId
            );
        }

        final String effectiveHierarchyId =
                hierarchyDataService.resolveEffectiveHierarchyId(
                        loginHierarchyId,
                        loginHierarchyType
                );

        log.info(
                "Fetching hierarchy details | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "effectiveHierarchyId={} | "
                        + "requestedHierarchyCode={}",
                loginHierarchyId,
                loginHierarchyType,
                effectiveHierarchyId,
                requestedHierarchyCode
        );

        List<String> hierarchyIds;

        if ("AD".equalsIgnoreCase(loginHierarchyType)) {

            hierarchyIds =
                    userDetailsRepository
                            .findHierarchyIdsByHierarchyCode(
                                    requestedHierarchyCode
                            );

        } else {

            /*
             * AU:
             * effectiveHierarchyId = parent AD
             *
             * WLAU:
             * effectiveHierarchyId = parent WL
             *
             * WL, SD, PT, DT, RT:
             * effectiveHierarchyId = own hierarchy ID
             */
            hierarchyIds =
                    accessHierarchyPathRepository
                            .findDescendantsByAncestorAndType(
                                    effectiveHierarchyId,
                                    requestedHierarchyCode
                            );
        }

        if (hierarchyIds == null || hierarchyIds.isEmpty()) {

            log.info(
                    "No hierarchy records found | "
                            + "effectiveHierarchyId={} | "
                            + "requestedHierarchyCode={}",
                    effectiveHierarchyId,
                    requestedHierarchyCode
            );

            return List.of();
        }

        List<HierarchyResponseDto> result =
                new ArrayList<>();

        for (String hierarchyId : hierarchyIds) {

            if (hierarchyId == null
                    || hierarchyId.isBlank()) {
                continue;
            }

            String type =
                    commonMethods.extractHierarchyType(
                            hierarchyId
                    );

            if (type == null || type.isBlank()) {

                log.warn(
                        "Unable to identify hierarchy type for ID: {}",
                        hierarchyId
                );

                continue;
            }

            switch (type.toUpperCase()) {

                case "RT" -> retailerRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                rt -> result.add(
                                        buildDto(
                                                rt.getRetailerId(),
                                                rt.getName(),
                                                rt.getMobileNumber(),
                                                rt.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Retailer record not found: {}",
                                        hierarchyId
                                )
                        );

                case "AD" -> adminRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                ad -> result.add(
                                        buildDto(
                                                ad.getId(),
                                                ad.getName(),
                                                ad.getMobileNumber(),
                                                ad.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Admin record not found: {}",
                                        hierarchyId
                                )
                        );

                case "DT" -> distributorRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                dt -> result.add(
                                        buildDto(
                                                dt.getDistributorId(),
                                                dt.getName(),
                                                dt.getMobileNumber(),
                                                dt.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Distributor record not found: {}",
                                        hierarchyId
                                )
                        );

                case "PT" -> partnerRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                pt -> result.add(
                                        buildDto(
                                                pt.getPartnerId(),
                                                pt.getName(),
                                                pt.getMobileNumber(),
                                                pt.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Partner record not found: {}",
                                        hierarchyId
                                )
                        );

                case "SD" -> superDistributorRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                sd -> result.add(
                                        buildDto(
                                                sd.getSuperDistributorId(),
                                                sd.getName(),
                                                sd.getMobileNumber(),
                                                sd.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Super Distributor record not found: {}",
                                        hierarchyId
                                )
                        );

                case "WL" -> whiteLabelRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                wl -> result.add(
                                        buildDto(
                                                wl.getWhiteLabelId(),
                                                wl.getName(),
                                                wl.getMobileNumber(),
                                                wl.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "White Label record not found: {}",
                                        hierarchyId
                                )
                        );

                case "ET" -> enterprisesRepository
                        .findById(hierarchyId)
                        .ifPresentOrElse(
                                et -> result.add(
                                        buildDto(
                                                et.getEnterpriseId(),
                                                et.getName(),
                                                et.getMobileNumber(),
                                                et.getEmailId()
                                        )
                                ),
                                () -> log.warn(
                                        "Enterprise record not found: {}",
                                        hierarchyId
                                )
                        );

                default -> log.warn(
                        "Unsupported hierarchy type | "
                                + "hierarchyId={} | type={}",
                        hierarchyId,
                        type
                );
            }
        }

        log.info(
                "Hierarchy details fetched successfully | "
                        + "requestedIds={} | returnedRecords={}",
                hierarchyIds.size(),
                result.size()
        );

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
