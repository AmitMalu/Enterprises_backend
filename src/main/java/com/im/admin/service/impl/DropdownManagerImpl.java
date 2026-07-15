package com.im.admin.service.impl;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.DropdownManagerService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
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
    private PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Autowired
    private HierarchyWalletService hierarchyWalletService;

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

        List<ChildUserDto> allChildUsers = new ArrayList<>();

        // Use access_hierarchy_path table (same logic as reports) instead of recursive traversal
        List<String> descendantIds = accessHierarchyPathRepository
                .findDescendantsByAncestor(parentHierarchyId);

        if (descendantIds != null) {
            for (String childId : descendantIds) {
                String childType = commonMethods.extractHierarchyType(childId);
                ChildUserDto dto = resolveChildDto(childId, childType);
                if (dto != null) {
                    allChildUsers.add(dto);
                }
            }
        }

        // ================= FILTERING =================
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
                .filter(u -> createdOn == null || (u.getCreatedOn() != null &&
                        u.getCreatedOn().toLocalDate().toString().equals(createdOn)))
                .sorted(Comparator.comparing(
                        ChildUserDto::getCreatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<ChildUserDto> paginated =
                (start >= filtered.size()) ? Collections.emptyList() : filtered.subList(start, end);

        return new PageImpl<>(paginated, pageable, filtered.size());
    }


//    private void fetchHierarchyRecursively(
//            String parentId,
//            String parentType,
//            List<ChildUserDto> result
//    ) {
//        // Fetch ALL children of this parent from user_details (type-agnostic)
//        List<UserDetails> children = userDetailsRepository.findByParentAccount(parentId);
//
//        for (UserDetails child : children) {
//            String childType = child.getHierarchyCode();
//            if (childType == null) continue;
//
//            // Map the child to ChildUserDto using the correct entity repo
//            ChildUserDto dto = resolveChildDto(child.getHierarchyId(), childType);
//            if (dto != null) {
//                result.add(dto);
//                // Recurse into this child's children
//                fetchHierarchyRecursively(child.getHierarchyId(), childType, result);
//            }
//        }
//    }

    private void fetchHierarchyRecursively(
            String parentId,
            String parentType,
            List<ChildUserDto> result
    ) {

        List<UserDetails> children =
                userDetailsRepository.findByParentAccount(parentId);

        for (UserDetails child : children) {

            String childHierarchyId = child.getHierarchyId();
            String childHierarchyCode = child.getHierarchyCode();

            if (childHierarchyId == null || childHierarchyCode == null) {
                continue;
            }

            ChildUserDto dto =
                    resolveChildDto(
                            childHierarchyId,
                            childHierarchyCode
                    );

            if (dto != null) {

                try {

                    Map<String, Object> walletInformation =
                            hierarchyWalletService.getWalletInformation(
                                    childHierarchyCode.toUpperCase(),
                                    childHierarchyId
                            );

                    dto.setWalletInformation(walletInformation);

                } catch (Exception exception) {

                    log.error(
                            "Unable to fetch wallet information for child hierarchyId: {}, hierarchyCode: {}",
                            childHierarchyId,
                            childHierarchyCode,
                            exception
                    );

                    dto.setWalletInformation(
                            createEmptyWalletInformation()
                    );
                }

                result.add(dto);

                fetchHierarchyRecursively(
                        childHierarchyId,
                        childHierarchyCode,
                        result
                );
            }
        }
    }

    /**
     * Resolves a ChildUserDto for any hierarchy type by looking up the correct entity repo.
     */
    private ChildUserDto resolveChildDto(String hierarchyId, String hierarchyCode) {
        try {
            return switch (hierarchyCode.toUpperCase()) {
                case "ET" -> enterprisesRepository.findByEnterpriseId(hierarchyId)
                        .map(this::mapEnterprise).orElse(null);
                case "WL" -> whiteLabelRepository.findByWhiteLabelId(hierarchyId)
                        .map(this::mapWhiteLabel).orElse(null);
                case "SD" -> superDistributorRepository.findBySuperDistributorId(hierarchyId)
                        .map(this::mapSuperDistributor).orElse(null);
                case "PT" -> partnerRepository.findByPartnerId(hierarchyId)
                        .map(this::mapPartner).orElse(null);
                case "DT" -> distributorRepository.findByDistributorId(hierarchyId)
                        .map(this::mapDistributor).orElse(null);
                case "RT" -> retailerRepository.findByRetailerId(hierarchyId)
                        .map(this::mapRetailer).orElse(null);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }


    private ChildUserDto mapPartner(Partner p) {
        return new ChildUserDto(
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
                p.getRejectionReason(),
                p.getCreatedOn(),
                null
        );
    }

    private ChildUserDto mapEnterprise(Enterprises e) {
        return new ChildUserDto(
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
                e.getRejectionReason(),
                e.getCreatedOn(),
                null
        );
    }


    private ChildUserDto mapWhiteLabel(WhiteLabels wl) {
        return new ChildUserDto(
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
                wl.getRejectionReason(),
                wl.getCreatedOn(),
                null
        );
    }

    private ChildUserDto mapSuperDistributor(SuperDistributor s) {
        return new ChildUserDto(
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
                s.getRejectionReason(),
                s.getCreatedOn(),
                null
        );
    }


    private ChildUserDto mapDistributor(Distributor d) {
        return new ChildUserDto(
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
                d.getRejectionReason(),
                d.getCreatedOn(),
                null
        );
    }

    private ChildUserDto mapRetailer(Retailer r) {
        return new ChildUserDto(
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
                r.getRejectionReason(),
                r.getCreatedOn(),
                null
        );
    }

    //  Helper method for safe string matching
    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }

    //  Updated hierarchy mapping
    private static final Map<String, List<String>> hierarchyChildren = Map.of(
            "ET", List.of("white_labels", "super_distributor", "partner", "distributor", "retailer"),
            "WL", List.of("super_distributor", "partner", "distributor", "retailer"),
            "SD", List.of("partner", "distributor", "retailer"),
            "PT", List.of("distributor", "retailer"),
            "DT", List.of("retailer")
    );


    @Override
    public Page<ChildUserDto> getChildUsersList(
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
        String childEntity = hierarchyChildrenList.get(parentType);

        if (childEntity == null)
            throw new RuntimeException("No child entity found for hierarchy type: " + parentType);

        // Step 1: Get all APPROVED child users for the hierarchy type
        List<ChildUserDto> approvedUsers = switch (childEntity) {
            case "enterprises" -> enterprisesRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                    .map(e -> new ChildUserDto(
                            "Enterprises", e.getEnterpriseId(), e.getParentAccount(), e.getAbbreviation(), e.getTitle(),
                            e.getName(), e.getMobileNumber(), e.getEmailId(), e.getDob(), e.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(e.getEnterpriseId()).map(UserDetails::getIsBlocked).orElse(false),
                            e.getRejectionReason(),
                            e.getCreatedOn(), null
                    )).toList();

            case "white_labels" -> whiteLabelRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(wl -> "APPROVED".equalsIgnoreCase(wl.getStatus()))
                    .map(wl -> new ChildUserDto(
                            "White Label", wl.getWhiteLabelId(), wl.getParentAccount(), wl.getAbbreviation(), wl.getTitle(),
                            wl.getName(), wl.getMobileNumber(), wl.getEmailId(), wl.getDob(), wl.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId()).map(UserDetails::getIsBlocked).orElse(false),
                            wl.getRejectionReason(),
                            wl.getCreatedOn(), null
                    )).toList();

            case "super_distributor" -> superDistributorRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                    .map(s -> new ChildUserDto(
                            "Super Distributor", s.getSuperDistributorId(), s.getParentAccount(), s.getAbbreviation(), s.getTitle(),
                            s.getName(), s.getMobileNumber(), s.getEmailId(), s.getDob(), s.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(s.getSuperDistributorId()).map(UserDetails::getIsBlocked).orElse(false),
                            s.getRejectionReason(),
                            s.getCreatedOn(), null
                    )).toList();

            case "partner" -> partnerRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                    .map(p -> new ChildUserDto(
                            "Partner", p.getPartnerId(), p.getParentAccount(), p.getAbbreviation(), p.getTitle(),
                            p.getName(), p.getMobileNumber(), p.getEmailId(), p.getDob(), p.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(p.getPartnerId()).map(UserDetails::getIsBlocked).orElse(false),
                            p.getRejectionReason(),
                            p.getCreatedOn(), null
                    )).toList();

            case "distributor" -> distributorRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                    .map(d -> new ChildUserDto(
                            "Distributor", d.getDistributorId(), d.getParentAccount(), d.getAbbreviation(), d.getTitle(),
                            d.getName(), d.getMobileNumber(), d.getEmailId(), d.getDob(), d.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(d.getDistributorId()).map(UserDetails::getIsBlocked).orElse(false),
                            d.getRejectionReason(),
                            d.getCreatedOn(), null
                    )).toList();

            case "retailer" -> retailerRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    .map(r -> new ChildUserDto(
                            "Retailer", r.getRetailerId(), r.getParentAccount(), r.getAbbreviation(), r.getTitle(),
                            r.getName(), r.getMobileNumber(), r.getEmailId(), r.getDob(), r.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(r.getRetailerId()).map(UserDetails::getIsBlocked).orElse(false),
                            r.getRejectionReason(),
                            r.getCreatedOn(), null
                    )).toList();

            default -> throw new RuntimeException("Invalid child entity type: " + childEntity);
        };

        // <-- PASTE THIS HERE
        approvedUsers.forEach(user -> {
            try {
                String hierarchyCode = commonMethods.extractHierarchyType(user.getUserId());

                user.setWalletInformation(
                        hierarchyWalletService.getWalletInformation(
                                hierarchyCode,
                                user.getUserId()
                        )
                );

            } catch (Exception e) {

                log.error("Unable to fetch wallet for {}", user.getUserId(), e);

                Map<String, Object> wallet = new LinkedHashMap<>();

                Map<String, Object> prepaid = new LinkedHashMap<>();
                prepaid.put("available", false);
                prepaid.put("message", "Wallet not found");

                Map<String, Object> postpaid = new LinkedHashMap<>();
                postpaid.put("available", false);
                postpaid.put("message", "Wallet not found");

                wallet.put("prepaid", prepaid);
                wallet.put("postpaid", postpaid);

                user.setWalletInformation(wallet);
            }
        });

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


    private static final Map<String, String> hierarchyChildrenList = Map.of(
            "SA", "admin",
            "AD", "enterprises",
            "ET", "white_labels",
            "WL", "super_distributor",
            "SD", "partner",
            "PT", "distributor",
            "DT", "retailer"
    );

    private Map<String, Object> createEmptyWalletInformation() {

        Map<String, Object> prepaid = new LinkedHashMap<>();
        prepaid.put("available", false);
        prepaid.put("message", "Unable to fetch prepaid wallet information");

        Map<String, Object> postpaid = new LinkedHashMap<>();
        postpaid.put("available", false);
        postpaid.put("message", "Unable to fetch postpaid wallet information");

        Map<String, Object> walletInformation = new LinkedHashMap<>();
        walletInformation.put("prepaid", prepaid);
        walletInformation.put("postpaid", postpaid);

        return walletInformation;
    }

}




