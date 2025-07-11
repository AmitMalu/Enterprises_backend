package com.im.admin.service.impl;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.*;
import com.im.admin.service.DropdownManagerService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class DropdownManagerImpl implements DropdownManagerService {

    @Autowired
    private CommonMethods commonMethods;

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
    public Page<ChildUserDto> getChildUsers(
            String parentHierarchyId,
            int page,
            int size,
            String userId,
            String parentAccount,
            String abbreviation,
            String title,
            String name,
            String mobileNumber,
            String emailId,
            String dob,
            String status,
            Boolean isBlocked,
            String network,
            String createdOn
    ) {
        String parentType = commonMethods.extractHierarchyType(parentHierarchyId);
        String childEntity = hierarchyChildren.get(parentType);

        if (childEntity == null)
            throw new RuntimeException("No child entity found for hierarchy type: " + parentType);

        // Step 1: Get all APPROVED child users for the hierarchy type
        List<ChildUserDto> approvedUsers = switch (childEntity) {
            case "enterprises" -> enterprisesRepository.findByParentAccountContaining(parentType).stream()
                    .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                    .map(e -> new ChildUserDto(
                            "Enterprises", e.getEnterpriseId(), e.getParentAccount(), e.getAbbreviation(), e.getTitle(),
                            e.getName(), e.getMobileNumber(), e.getEmailId(), e.getDob(), e.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(e.getEnterpriseId()).map(UserDetails::getIsBlocked).orElse(false), e.getCreatedOn()
                    )).toList();

            case "white_labels" -> whiteLabelRepository.findByParentAccountContaining(parentType).stream()
                    .filter(wl -> "APPROVED".equalsIgnoreCase(wl.getStatus()))
                    .map(wl -> new ChildUserDto(
                            "White Label", wl.getWhiteLabelId(), wl.getParentAccount(), wl.getAbbreviation(), wl.getTitle(),
                            wl.getName(), wl.getMobileNumber(), wl.getEmailId(), wl.getDob(), wl.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId()).map(UserDetails::getIsBlocked).orElse(false), wl.getCreatedOn()
                    )).toList();

            case "super_distributor" -> superDistributorRepository.findByParentAccountContaining(parentType).stream()
                    .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                    .map(s -> new ChildUserDto(
                            "Super Distributor", s.getSuperDistributorId(), s.getParentAccount(), s.getAbbreviation(), s.getTitle(),
                            s.getName(), s.getMobileNumber(), s.getEmailId(), s.getDob(), s.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(s.getSuperDistributorId()).map(UserDetails::getIsBlocked).orElse(false), s.getCreatedOn()
                    )).toList();

            case "partner" -> partnerRepository.findByParentAccountContaining(parentType).stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                    .map(p -> new ChildUserDto(
                            "Partner", p.getPartnerId(), p.getParentAccount(), p.getAbbreviation(), p.getTitle(),
                            p.getName(), p.getMobileNumber(), p.getEmailId(), p.getDob(), p.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(p.getPartnerId()).map(UserDetails::getIsBlocked).orElse(false), p.getCreatedOn()
                    )).toList();

            case "distributor" -> distributorRepository.findByParentAccountContaining(parentType).stream()
                    .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                    .map(d -> new ChildUserDto(
                            "Distributor", d.getDistributorId(), d.getParentAccount(), d.getAbbreviation(), d.getTitle(),
                            d.getName(), d.getMobileNumber(), d.getEmailId(), d.getDob(), d.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(d.getDistributorId()).map(UserDetails::getIsBlocked).orElse(false), d.getCreatedOn()
                    )).toList();

            case "retailer" -> retailerRepository.findByParentAccountContaining(parentType).stream()
                    .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    .map(r -> new ChildUserDto(
                            "Retailer", r.getRetailerId(), r.getParentAccount(), r.getAbbreviation(), r.getTitle(),
                            r.getName(), r.getMobileNumber(), r.getEmailId(), r.getDob(), r.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(r.getRetailerId()).map(UserDetails::getIsBlocked).orElse(false), r.getCreatedOn()
                    )).toList();

            default -> throw new RuntimeException("Invalid child entity type: " + childEntity);
        };

        // Step 2: Apply filters
        List<ChildUserDto> filtered = approvedUsers.stream()
                .filter(u -> userId == null || (u.getUserId() != null && u.getUserId().toLowerCase().contains(userId.toLowerCase())))
                .filter(u -> parentAccount == null || (u.getParentAccount() != null && u.getParentAccount().toLowerCase().contains(parentAccount.toLowerCase())))
                .filter(u -> abbreviation == null || (u.getAbbreviation() != null && u.getAbbreviation().toLowerCase().contains(abbreviation.toLowerCase())))
                .filter(u -> title == null || (u.getTitle() != null && u.getTitle().toLowerCase().contains(title.toLowerCase())))
                .filter(u -> name == null || (u.getName() != null && u.getName().toLowerCase().contains(name.toLowerCase())))
                .filter(u -> mobileNumber == null || (u.getMobileNumber() != null && u.getMobileNumber().contains(mobileNumber)))
                .filter(u -> emailId == null || (u.getEmailId() != null && u.getEmailId().toLowerCase().contains(emailId.toLowerCase())))
                .filter(u -> status == null || (u.getStatus() != null && u.getStatus().toLowerCase().contains(status.toLowerCase())))
                .filter(u -> dob == null || (u.getDob() != null && u.getDob().toString().equals(dob)))
                .filter(u -> isBlocked == null || (u.getIsBlocked() != null && u.getIsBlocked().equals(isBlocked)))
                .filter(u -> network == null || (u.getNetwork() != null && u.getNetwork().toLowerCase().contains(network.toLowerCase())))
                .filter(u -> createdOn == null || (u.getCreatedOn() != null && u.getCreatedOn().toLocalDate().toString().equals(createdOn)))
                .collect(Collectors.toList());

        // Step 3: Pagination
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<ChildUserDto> paginated = (start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

        return new PageImpl<>(paginated, pageable, filtered.size());
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
