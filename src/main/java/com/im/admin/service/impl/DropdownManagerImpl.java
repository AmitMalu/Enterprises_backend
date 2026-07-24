package com.im.admin.service.impl;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.dto.WalletBalanceMaps;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.service.DropdownManagerService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Autowired
    private EnterprisesPostpaidAmountRepository enterprisesPostpaidAmountRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository enterprisesPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository distributorPostpaidAmountRepository;

    @Autowired
    private DistributorPrepaidAmountRepository distributorPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

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

        WalletBalanceMaps walletMaps = loadAllWalletBalances();

        // Use access_hierarchy_path table (same logic as reports) instead of recursive traversal
        List<String> descendantIds = accessHierarchyPathRepository
                .findDescendantsByAncestor(parentHierarchyId);

        if (descendantIds != null) {
            for (String childId : descendantIds) {
                String childType = commonMethods.extractHierarchyType(childId);
                ChildUserDto dto = resolveChildDto(childId, childType, walletMaps);
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

    /**
     * Resolves a ChildUserDto for any hierarchy type by looking up the correct entity repo.
     */
    private ChildUserDto resolveChildDto(
            String hierarchyId,
            String hierarchyCode,
            WalletBalanceMaps walletMaps) {

        try {

            return switch (hierarchyCode.toUpperCase()) {

                case "ET" -> enterprisesRepository.findByEnterpriseId(hierarchyId)
                        .map(e -> mapEnterprise(e, walletMaps))
                        .orElse(null);

                case "WL" -> whiteLabelRepository.findByWhiteLabelId(hierarchyId)
                        .map(w -> mapWhiteLabel(w, walletMaps))
                        .orElse(null);

                case "SD" -> superDistributorRepository.findBySuperDistributorId(hierarchyId)
                        .map(sd -> mapSuperDistributor(sd, walletMaps))
                        .orElse(null);

                case "PT" -> partnerRepository.findByPartnerId(hierarchyId)
                        .map(p -> mapPartner(p, walletMaps))
                        .orElse(null);

                case "DT" -> distributorRepository.findByDistributorId(hierarchyId)
                        .map(d -> mapDistributor(d, walletMaps))
                        .orElse(null);

                case "RT" -> retailerRepository.findByRetailerId(hierarchyId)
                        .map(r -> mapRetailer(r, walletMaps))
                        .orElse(null);

                default -> null;
            };

        } catch (Exception e) {
            return null;
        }
    }


    private ChildUserDto mapPartner(
            Partner p,
            WalletBalanceMaps walletMaps) {

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
                        .map(UserDetails::getIsBlocked)
                        .orElse(false),
                p.getRejectionReason(),
                p.getCreatedOn(),
                walletMaps.getPrepaid().getOrDefault(p.getPartnerId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(p.getPartnerId(), BigDecimal.ZERO)
        );
    }

    private ChildUserDto mapEnterprise(
            Enterprises e,
            WalletBalanceMaps walletMaps) {

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
                        .map(UserDetails::getIsBlocked)
                        .orElse(false),
                e.getRejectionReason(),
                e.getCreatedOn(),
                walletMaps.getPrepaid().getOrDefault(e.getEnterpriseId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(e.getEnterpriseId(), BigDecimal.ZERO)
        );
    }


    private ChildUserDto mapWhiteLabel(
            WhiteLabels wl,
            WalletBalanceMaps walletMaps) {

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
                        .map(UserDetails::getIsBlocked)
                        .orElse(false),
                wl.getRejectionReason(),
                wl.getCreatedOn(),
                walletMaps.getPrepaid().getOrDefault(wl.getWhiteLabelId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(wl.getWhiteLabelId(), BigDecimal.ZERO)
        );
    }

    private ChildUserDto mapSuperDistributor(
            SuperDistributor s,
            WalletBalanceMaps walletMaps) {

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
                walletMaps.getPrepaid().getOrDefault(s.getSuperDistributorId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(s.getSuperDistributorId(), BigDecimal.ZERO)
        );
    }

    private ChildUserDto mapDistributor(
            Distributor d,
            WalletBalanceMaps walletMaps) {

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
                walletMaps.getPrepaid().getOrDefault(d.getDistributorId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(d.getDistributorId(), BigDecimal.ZERO)
        );
    }

    private ChildUserDto mapRetailer(
            Retailer r,
            WalletBalanceMaps walletMaps) {

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
                walletMaps.getPrepaid().getOrDefault(r.getRetailerId(), BigDecimal.ZERO),
                walletMaps.getPostpaid().getOrDefault(r.getRetailerId(), BigDecimal.ZERO)
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
                            e.getCreatedOn(), null,null
                    )).toList();

            case "white_labels" -> whiteLabelRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(wl -> "APPROVED".equalsIgnoreCase(wl.getStatus()))
                    .map(wl -> new ChildUserDto(
                            "White Label", wl.getWhiteLabelId(), wl.getParentAccount(), wl.getAbbreviation(), wl.getTitle(),
                            wl.getName(), wl.getMobileNumber(), wl.getEmailId(), wl.getDob(), wl.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId()).map(UserDetails::getIsBlocked).orElse(false),
                            wl.getRejectionReason(),
                            wl.getCreatedOn(), null,null
                    )).toList();

            case "super_distributor" -> superDistributorRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()))
                    .map(s -> new ChildUserDto(
                            "Super Distributor", s.getSuperDistributorId(), s.getParentAccount(), s.getAbbreviation(), s.getTitle(),
                            s.getName(), s.getMobileNumber(), s.getEmailId(), s.getDob(), s.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(s.getSuperDistributorId()).map(UserDetails::getIsBlocked).orElse(false),
                            s.getRejectionReason(),
                            s.getCreatedOn(), null,null
                    )).toList();

            case "partner" -> partnerRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                    .map(p -> new ChildUserDto(
                            "Partner", p.getPartnerId(), p.getParentAccount(), p.getAbbreviation(), p.getTitle(),
                            p.getName(), p.getMobileNumber(), p.getEmailId(), p.getDob(), p.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(p.getPartnerId()).map(UserDetails::getIsBlocked).orElse(false),
                            p.getRejectionReason(),
                            p.getCreatedOn(), null,null
                    )).toList();

            case "distributor" -> distributorRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus()))
                    .map(d -> new ChildUserDto(
                            "Distributor", d.getDistributorId(), d.getParentAccount(), d.getAbbreviation(), d.getTitle(),
                            d.getName(), d.getMobileNumber(), d.getEmailId(), d.getDob(), d.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(d.getDistributorId()).map(UserDetails::getIsBlocked).orElse(false),
                            d.getRejectionReason(),
                            d.getCreatedOn(), null,null
                    )).toList();

            case "retailer" -> retailerRepository.findByParentAccount(parentHierarchyId).stream()
                    .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    .map(r -> new ChildUserDto(
                            "Retailer", r.getRetailerId(), r.getParentAccount(), r.getAbbreviation(), r.getTitle(),
                            r.getName(), r.getMobileNumber(), r.getEmailId(), r.getDob(), r.getStatus(),
                            userDetailsRepository.findFirstByHierarchyId(r.getRetailerId()).map(UserDetails::getIsBlocked).orElse(false),
                            r.getRejectionReason(),
                            r.getCreatedOn(), null,null
                    )).toList();

            default -> throw new RuntimeException("Invalid child entity type: " + childEntity);
        };

        // <-- PASTE THIS HERE
        approvedUsers.forEach(user -> {
            try {

                String hierarchyCode =
                        commonMethods.extractHierarchyType(user.getUserId());

                Map<String, Object> wallet =
                        hierarchyWalletService.getWalletInformation(
                                hierarchyCode,
                                user.getUserId()
                        );

                // Default values
                user.setPrepaidBalance(BigDecimal.ZERO);
                user.setPostpaidBalance(BigDecimal.ZERO);

                // Prepaid Balance
                Object prepaidObj = wallet.get("prepaid");
                if (prepaidObj instanceof Map<?, ?> prepaidMap) {

                    Object balance = prepaidMap.get("availableBalance");

                    if (balance instanceof BigDecimal bd) {
                        user.setPrepaidBalance(bd);
                    } else if (balance != null) {
                        user.setPrepaidBalance(new BigDecimal(balance.toString()));
                    }
                }

                // Postpaid Balance
                Object postpaidObj = wallet.get("postpaid");
                if (postpaidObj instanceof Map<?, ?> postpaidMap) {

                    Object balance = postpaidMap.get("availableBalance");

                    if (balance instanceof BigDecimal bd) {
                        user.setPostpaidBalance(bd);
                    } else if (balance != null) {
                        user.setPostpaidBalance(new BigDecimal(balance.toString()));
                    }
                }

            } catch (Exception e) {

                log.error("Unable to fetch wallet for {}", user.getUserId(), e);

                user.setPrepaidBalance(BigDecimal.ZERO);
                user.setPostpaidBalance(BigDecimal.ZERO);
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

    private WalletBalanceMaps loadAllWalletBalances() {

        WalletBalanceMaps maps = new WalletBalanceMaps();

        // Retailer
        retailerPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getRetailerId().getRetailerId(),w.getAvailableBalance()));

        retailerPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getRetailerId().getRetailerId(),w.getAvailableBalance()));

        // Distributor
        distributorPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getDistributorId().getDistributorId(),w.getAvailableBalance()));

        distributorPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getDistributorId().getDistributorId(),w.getAvailableBalance()));

        // Partner
        partnerPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getPartnerId().getPartnerId(),w.getAvailableBalance()));

        partnerPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getPartnerId().getPartnerId(),w.getAvailableBalance()));

        // Super Distributor
        superDistributorPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getSuperDistributorId().getSuperDistributorId(),w.getAvailableBalance()));

        superDistributorPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getSuperDistributorId().getSuperDistributorId(),w.getAvailableBalance()));

        // White Label
        whiteLabelPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getWhiteLabelId().getWhiteLabelId(),w.getAvailableBalance()));

        whiteLabelPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getWhiteLabelId().getWhiteLabelId(),w.getAvailableBalance()));

        // Enterprise
        enterprisesPrepaidAmountRepository.findAll().forEach(w ->
                maps.getPrepaid().put(w.getEnterprise().getEnterpriseId(),w.getAvailableBalance()));

        enterprisesPostpaidAmountRepository.findAll().forEach(w ->
                maps.getPostpaid().put(w.getEnterprise().getEnterpriseId(),w.getAvailableBalance()));

        return maps;
    }

}




