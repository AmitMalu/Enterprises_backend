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
import java.util.stream.Stream;


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
        // Extract parent type (e.g., "WL" -> WhiteLabel)
        String parentType = commonMethods.extractHierarchyType(parentHierarchyId);
        List<String> childEntities = hierarchyChildren.get(parentType);

        if (childEntities == null || childEntities.isEmpty())
            throw new RuntimeException("No child entities found for hierarchy type: " + parentType);

        List<ChildUserDto> allChildUsers = childEntities.stream()
                .flatMap(childEntity -> switch (childEntity) {
                    case "enterprises" -> enterprisesRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                            .map(e -> new ChildUserDto(
                                    "Enterprises",
                                    e.getEnterpriseId(),
                                    e.getParentAccount(),
                                    e.getAbbreviation(),
                                    e.getTitle(),
                                    e.getName(),
                                    e.getMobileNumber(),
                                    e.getEmailId(),
                                    e.getDob(),
                                    e.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(e.getEnterpriseId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    e.getCreatedOn()
                            ));

                    case "white_labels" -> whiteLabelRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(wl -> "APPROVED".equalsIgnoreCase(wl.getStatus()))
                            .map(wl -> new ChildUserDto(
                                    "White Label",
                                    wl.getWhiteLabelId(),
                                    wl.getParentAccount(),
                                    wl.getAbbreviation(),
                                    wl.getTitle(),
                                    wl.getName(),
                                    wl.getMobileNumber(),
                                    wl.getEmailId(),
                                    wl.getDob(),
                                    wl.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    wl.getCreatedOn()
                            ));

                    case "super_distributor" -> superDistributorRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                            .map(s -> new ChildUserDto(
                                    "Super Distributor",
                                    s.getSuperDistributorId(),
                                    s.getParentAccount(),
                                    s.getAbbreviation(),
                                    s.getTitle(),
                                    s.getName(),
                                    s.getMobileNumber(),
                                    s.getEmailId(),
                                    s.getDob(),
                                    s.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(s.getSuperDistributorId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    s.getCreatedOn()
                            ));

                    case "partner" -> partnerRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                            .map(p -> new ChildUserDto(
                                    "Partner",
                                    p.getPartnerId(),
                                    p.getParentAccount(),
                                    p.getAbbreviation(),
                                    p.getTitle(),
                                    p.getName(),
                                    p.getMobileNumber(),
                                    p.getEmailId(),
                                    p.getDob(),
                                    p.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(p.getPartnerId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    p.getCreatedOn()
                            ));

                    case "distributor" -> distributorRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                            .map(d -> new ChildUserDto(
                                    "Distributor",
                                    d.getDistributorId(),
                                    d.getParentAccount(),
                                    d.getAbbreviation(),
                                    d.getTitle(),
                                    d.getName(),
                                    d.getMobileNumber(),
                                    d.getEmailId(),
                                    d.getDob(),
                                    d.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(d.getDistributorId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    d.getCreatedOn()
                            ));

                    case "retailer" -> retailerRepository.findByParentAccount(parentHierarchyId).stream()
                            .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                            .map(r -> new ChildUserDto(
                                    "Retailer",
                                    r.getRetailerId(),
                                    r.getParentAccount(),
                                    r.getAbbreviation(),
                                    r.getTitle(),
                                    r.getName(),
                                    r.getMobileNumber(),
                                    r.getEmailId(),
                                    r.getDob(),
                                    r.getStatus(),
                                    userDetailsRepository.findFirstByHierarchyId(r.getRetailerId())
                                            .map(UserDetails::getIsBlocked).orElse(false),
                                    r.getCreatedOn()
                            ));

                    default -> Stream.empty();
                }).collect(Collectors.toList());

        // Step 2: Apply filters
        List<ChildUserDto> filtered = allChildUsers.stream()
                .filter(u -> userId == null || containsIgnoreCase(u.getUserId(), userId))
                .filter(u -> parentAccount == null || containsIgnoreCase(u.getParentAccount(), parentAccount))
                .filter(u -> abbreviation == null || containsIgnoreCase(u.getAbbreviation(), abbreviation))
                .filter(u -> title == null || containsIgnoreCase(u.getTitle(), title))
                .filter(u -> name == null || containsIgnoreCase(u.getName(), name))
                .filter(u -> mobileNumber == null || containsIgnoreCase(u.getMobileNumber(), mobileNumber))
                .filter(u -> emailId == null || containsIgnoreCase(u.getEmailId(), emailId))
                .filter(u -> status == null || containsIgnoreCase(u.getStatus(), status))
                .filter(u -> dob == null || (u.getDob() != null && u.getDob().toString().equals(dob)))
                .filter(u -> isBlocked == null || u.getIsBlocked().equals(isBlocked))
                .filter(u -> network == null || containsIgnoreCase(u.getNetwork(), network))
                .filter(u -> createdOn == null || (u.getCreatedOn() != null && u.getCreatedOn().toLocalDate().toString().equals(createdOn)))
                .collect(Collectors.toList());

        // Step 3: Pagination
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ChildUserDto> paginated = (start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

        return new PageImpl<>(paginated, pageable, filtered.size());
    }

    // ✅ Helper method for safe string matching
    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    // ✅ Updated hierarchy mapping
    private static final Map<String, List<String>> hierarchyChildren = Map.of(
            "ET", List.of("white_labels", "super_distributor", "partner", "distributor", "retailer"),
            "WL", List.of("super_distributor", "partner", "distributor", "retailer"),
            "SD", List.of("partner", "distributor", "retailer"),
            "PT", List.of("distributor", "retailer"),
            "DT", List.of("retailer")
    );


}
