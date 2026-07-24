package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.EmailType;
import com.im.admin.exception.NoPendingRecordsFoundException;
import com.im.admin.repositories.*;
import com.im.admin.request.ChangeStatusToPendingRequest;
import com.im.admin.request.UserContext;
import com.im.admin.response.GenericPendingResponseDto;
import com.im.admin.service.EmailService;
import com.im.admin.service.OnboardingService;
import com.im.admin.service.asyncMethods.AsyncPendingFetcher;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.HierarchyAccessValidator;
import com.im.admin.util.PasswordGeneratorUtil;
import com.im.admin.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private AsyncPendingFetcher asyncPendingFetcher;

    @Autowired
    private ImSubRoleRepository imSubRoleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private EmailCredentialsRepository emailCredentialsRepository;

    @Autowired
    private HierarchyAccessValidator hierarchyAccessValidator;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    private static final Map<String, String>
            DELEGATED_PARENT_TYPES = Map.of(
            "AU", "AD",
            "WLAU", "WL"
    );

    @Override
    @Transactional(readOnly = true)
    public Page<GenericPendingRecordDto> getAllRecordsByStatus(
            String status,
            int page,
            int size,
            String type,
            String id,
            String name,
            String createdBy,
            String createdOn,
            String mobileNumber,
            String emailId,
            String parentAccount,
            String rejectionReason
    ) {

        log.info(
                "===== FETCHING USER RECORDS BY STATUS ====="
        );

        log.info(
                "Request filters | status={} | page={} | size={} | "
                        + "type={} | id={} | name={}",
                status,
                page,
                size,
                type,
                id,
                name
        );

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Page size must be greater than zero"
            );
        }

        UserContext ctx =
                tokenUtil.getUserContext();

        if (ctx == null) {
            throw new IllegalStateException(
                    "Unable to obtain logged-in user context"
            );
        }

        String loginHierarchyId =
                ctx.getHierarchyId();

        if (!CommonMethods.hasText(loginHierarchyId)) {
            throw new IllegalStateException(
                    "Login hierarchy ID is missing from token"
            );
        }

        loginHierarchyId =
                loginHierarchyId.trim();

        String loginHierarchyType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        if (!CommonMethods.hasText(loginHierarchyType)) {
            throw new IllegalStateException(
                    "Unable to determine hierarchy type for: "
                            + loginHierarchyId
            );
        }

        loginHierarchyType =
                loginHierarchyType
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String accessRoot =
                resolveAccessRoot(
                        loginHierarchyId,
                        loginHierarchyType
                );

        boolean fullSystemAccess =
                commonMethods.hasFullSystemAccess(
                        loginHierarchyType
                );

        log.info(
                "Hierarchy access resolved | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "accessRoot={} | "
                        + "fullSystemAccess={}",
                loginHierarchyId,
                loginHierarchyType,
                accessRoot,
                fullSystemAccess
        );

        /*
         * For non-AD users, load the complete accessible hierarchy
         * only once.
         *
         * HashSet gives fast contains() checking for every record.
         */
        Set<String> accessibleHierarchyIds;

        if (fullSystemAccess) {

            accessibleHierarchyIds =
                    Collections.emptySet();

        } else {

            List<String> descendantIds =
                    accessHierarchyPathRepository
                            .findAllDescendantIds(
                                    accessRoot
                            );

            accessibleHierarchyIds =
                    descendantIds == null
                            ? Collections.emptySet()
                            : descendantIds.stream()
                            .filter(this::hasText)
                            .map(String::trim)
                            .collect(Collectors.toSet());

            /*
             * Defensive fallback:
             *
             * The closure table should already contain:
             *
             * accessRoot -> accessRoot -> depth 0
             *
             * We still add it to avoid accidental denial if the
             * depth-zero row is temporarily missing.
             */
            accessibleHierarchyIds.add(accessRoot);

            log.info(
                    "Accessible hierarchy IDs loaded | "
                            + "accessRoot={} | count={}",
                    accessRoot,
                    accessibleHierarchyIds.size()
            );
        }

        /*
         * Start all entity fetches concurrently.
         */
        CompletableFuture<List<Retailer>>
                retailersFuture =
                asyncPendingFetcher.fetchRetailers(
                        status
                );

        CompletableFuture<List<Distributor>>
                distributorsFuture =
                asyncPendingFetcher.fetchDistributors(
                        status
                );

        CompletableFuture<List<Partner>>
                partnersFuture =
                asyncPendingFetcher.fetchPartners(
                        status
                );

        CompletableFuture<List<SuperDistributor>>
                superDistributorsFuture =
                asyncPendingFetcher.fetchSuperDistributors(
                        status
                );

        CompletableFuture<List<WhiteLabels>>
                whiteLabelsFuture =
                asyncPendingFetcher.fetchWhiteLabels(
                        status
                );

        CompletableFuture<List<Enterprises>>
                enterprisesFuture =
                asyncPendingFetcher.fetchEnterprises(
                        status
                );

        try {

            CompletableFuture.allOf(
                    retailersFuture,
                    distributorsFuture,
                    partnersFuture,
                    superDistributorsFuture,
                    whiteLabelsFuture,
                    enterprisesFuture
            ).join();

        } catch (CompletionException ex) {

            log.error(
                    "Failed to fetch user records asynchronously",
                    ex
            );

            throw new IllegalStateException(
                    "Unable to fetch user records",
                    ex.getCause() != null
                            ? ex.getCause()
                            : ex
            );
        }

        List<Retailer> retailers =
                safeFutureResult(
                        retailersFuture,
                        "Retailers"
                );

        List<Distributor> distributors =
                safeFutureResult(
                        distributorsFuture,
                        "Distributors"
                );

        List<Partner> partners =
                safeFutureResult(
                        partnersFuture,
                        "Partners"
                );

        List<SuperDistributor> superDistributors =
                safeFutureResult(
                        superDistributorsFuture,
                        "SuperDistributors"
                );

        List<WhiteLabels> whiteLabels =
                safeFutureResult(
                        whiteLabelsFuture,
                        "WhiteLabels"
                );

        List<Enterprises> enterprises =
                safeFutureResult(
                        enterprisesFuture,
                        "Enterprises"
                );

        log.info(
                "Fetched entity counts | retailers={} | "
                        + "distributors={} | partners={} | "
                        + "superDistributors={} | whiteLabels={} | "
                        + "enterprises={}",
                retailers.size(),
                distributors.size(),
                partners.size(),
                superDistributors.size(),
                whiteLabels.size(),
                enterprises.size()
        );

        List<Object> allEntities =
                Stream.of(
                                retailers,
                                distributors,
                                partners,
                                superDistributors,
                                whiteLabels,
                                enterprises
                        )
                        .flatMap(Collection::stream)
                        .map(entity -> (Object) entity)
                        .toList();

        log.info(
                "Total entities before hierarchy filtering: {}",
                allEntities.size()
        );

        final String finalLoginHierarchyId =
                loginHierarchyId;

        final String finalLoginHierarchyType =
                loginHierarchyType;

        final String finalAccessRoot =
                accessRoot;

        final Set<String> finalAccessibleHierarchyIds =
                accessibleHierarchyIds;

        /*
         * Apply hierarchy authorization.
         */
        List<GenericPendingRecordDto> hierarchyScopedRecords =
                allEntities.stream()
                        .filter(Objects::nonNull)
                        .filter(entity -> {

                            String recordHierarchyId =
                                    extractId(entity);

                            if (!CommonMethods.hasText(recordHierarchyId)) {

                                log.warn(
                                        "Skipping entity because hierarchy ID "
                                                + "could not be extracted | "
                                                + "entityType={}",
                                        entity.getClass()
                                                .getSimpleName()
                                );

                                return false;
                            }

                            recordHierarchyId =
                                    recordHierarchyId.trim();

                            /*
                             * AD has complete system visibility.
                             */
                            if (fullSystemAccess) {
                                return true;
                            }

                            boolean belongs =
                                    finalAccessibleHierarchyIds
                                            .contains(
                                                    recordHierarchyId
                                            );

                            log.debug(
                                    "Hierarchy authorization | "
                                            + "loginHierarchyId={} | "
                                            + "loginHierarchyType={} | "
                                            + "accessRoot={} | "
                                            + "recordHierarchyId={} | "
                                            + "allowed={}",
                                    finalLoginHierarchyId,
                                    finalLoginHierarchyType,
                                    finalAccessRoot,
                                    recordHierarchyId,
                                    belongs
                            );

                            return belongs;
                        })
                        .map(this::mapToDto)
                        .filter(Objects::nonNull)
                        .toList();

        log.info(
                "Records after hierarchy filtering: {}",
                hierarchyScopedRecords.size()
        );

        /*
         * Apply frontend search filters.
         *
         * All checks are null-safe.
         */
        List<GenericPendingRecordDto> filteredRecords =
                hierarchyScopedRecords.stream()

                        .filter(record ->
                                matchesExactIgnoreCase(
                                        record.getStatus(),
                                        status
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getType(),
                                        type
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getId(),
                                        id
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getName(),
                                        name
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getCreatedBy(),
                                        createdBy
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getCreatedOn(),
                                        createdOn
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getMobileNumber(),
                                        mobileNumber
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getEmailId(),
                                        emailId
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getParentAccount(),
                                        parentAccount
                                )
                        )

                        .filter(record ->
                                containsIgnoreCase(
                                        record.getRemarks(),
                                        rejectionReason
                                )
                        )

                        .sorted(
                                Comparator.comparing(
                                                GenericPendingRecordDto::getCreatedOn,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                        .reversed()
                        )
                        .toList();

        log.info(
                "Records after search filters: {}",
                filteredRecords.size()
        );

        /*
         * Do not throw NoPendingRecordsFoundException before pagination.
         *
         * Returning an empty page is normally easier for the frontend
         * and works correctly when a requested page is beyond the last
         * available page.
         */
        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        int start =
                Math.toIntExact(
                        pageable.getOffset()
                );

        if (start >= filteredRecords.size()) {

            log.info(
                    "No records available for requested page | "
                            + "page={} | size={} | totalRecords={}",
                    page,
                    size,
                    filteredRecords.size()
            );

            return new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    filteredRecords.size()
            );
        }

        int end =
                Math.min(
                        start + pageable.getPageSize(),
                        filteredRecords.size()
                );

        List<GenericPendingRecordDto> paginated =
                filteredRecords.subList(
                        start,
                        end
                );

        log.info(
                "Returning pending records | "
                        + "page={} | size={} | "
                        + "pageElements={} | totalElements={}",
                page,
                size,
                paginated.size(),
                filteredRecords.size()
        );

        log.info(
                "===== FETCH USER RECORDS COMPLETED ====="
        );

        return new PageImpl<>(
                paginated,
                pageable,
                filteredRecords.size()
        );
    }

    private String extractId(Object entity) {

        if (entity == null) return null;

        if (entity instanceof Retailer r) return r.getRetailerId();
        if (entity instanceof Distributor d) return d.getDistributorId();
        if (entity instanceof Partner p) return p.getPartnerId();
        if (entity instanceof SuperDistributor sd) return sd.getSuperDistributorId();
        if (entity instanceof WhiteLabels wl) return wl.getWhiteLabelId();
        if (entity instanceof Enterprises e) return e.getEnterpriseId();
        if (entity instanceof Admin a) return a.getId();

        return null;
    }

    private GenericPendingRecordDto mapToDto(Object entity) {

        if (entity instanceof Retailer r) return PendingRecordMapper.fromRetailer(r);
        if (entity instanceof Distributor d) return PendingRecordMapper.fromDistributor(d);
        if (entity instanceof Partner p) return PendingRecordMapper.fromPartner(p);
        if (entity instanceof SuperDistributor s) return PendingRecordMapper.fromSuperDistributor(s);
        if (entity instanceof WhiteLabels w) return PendingRecordMapper.fromWhiteLabel(w);
        if (entity instanceof Enterprises e) return PendingRecordMapper.fromEnterprise(e);

        return null;
    }

    @Override
    @Transactional
    public String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto) {

        String loginId = tokenUtil.extractHierarchyId();

        String response = null;
        String hierarchyId = hierarchyRequestDto.getHierarchyId();
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        boolean isApproved = hierarchyRequestDto.getIsApproved();
        String remarks = hierarchyRequestDto.getRemarks();

        switch (hierarchyType) {
            case "RT" -> {
                Optional<Retailer> rtOpt = retailerRepository.findById(hierarchyId);
                if(rtOpt.isEmpty()) return "Retailer not found";
                Retailer rt = rtOpt.get();
                if (!"PENDING".equalsIgnoreCase(rt.getStatus())) return "Already processed";
                if (isApproved) {
                    rt.setStatus("APPROVED");
                    rt.setRemarks(remarks);
                    rt.setApprovedBy(loginId); ///get from amaan
                    rt.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, rt.getRetailerId(), rt.getMobileNumber(), rt.getEmailId(), rt.getName(),rt.getParentAccount(), rt.getCreatedBy(), loginId);
                } else {
                    rt.setStatus("REJECTED");
                    rt.setRejectionReason(remarks);
                }
                retailerRepository.save(rt);
            }
            case "DT" -> {
                Optional<Distributor> dtOpt = distributorRepository.findById(hierarchyId);
                if(dtOpt.isEmpty()) return "Distributor not found";
                Distributor dt = dtOpt.get();
                if (!"PENDING".equalsIgnoreCase(dt.getStatus())) return "Already processed";
                if (isApproved) {
                    dt.setStatus("APPROVED");
                    dt.setRemarks(remarks);
                    dt.setApprovedBy(loginId);
                    dt.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, dt.getDistributorId(), dt.getMobileNumber(), dt.getEmailId(), dt.getName(),dt.getParentAccount(),dt.getCreatedBy(),loginId);
                } else {
                    dt.setStatus("REJECTED");
                    dt.setRejectionReason(remarks);
                }
                distributorRepository.save(dt);
            }

            case "PT" -> {
                Optional<Partner> ptOpt = partnerRepository.findById(hierarchyId);
                if (ptOpt.isEmpty()) return "Partner not found";
                Partner pt = ptOpt.get();
                if (!"PENDING".equalsIgnoreCase(pt.getStatus())) return "Already processed";
                if (isApproved) {
                    pt.setStatus("APPROVED");
                    pt.setRemarks(remarks);
                    pt.setApprovedBy(loginId);
                    pt.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, pt.getPartnerId(), pt.getMobileNumber(), pt.getEmailId(), pt.getName(),pt.getParentAccount(),pt.getCreatedBy(),loginId);
                } else {
                    pt.setStatus("REJECTED");
                    pt.setRejectionReason(remarks);
                }
                partnerRepository.save(pt);
            }

            case "SD" -> {
                Optional<SuperDistributor> sdOpt = superDistributorRepository.findById(hierarchyId);
                if (sdOpt.isEmpty()) return "SuperDistributor not found";
                SuperDistributor sd = sdOpt.get();
                if (!"PENDING".equalsIgnoreCase(sd.getStatus())) return "Already processed";
                if (isApproved) {
                    sd.setStatus("APPROVED");
                    sd.setRemarks(remarks);
                    sd.setApprovedBy(loginId);
                    sd.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, sd.getSuperDistributorId(), sd.getMobileNumber(), sd.getEmailId(), sd.getName(),sd.getParentAccount(),sd.getCreatedBy(),loginId);
                } else {
                    sd.setStatus("REJECTED");
                    sd.setRejectionReason(remarks);
                }
                superDistributorRepository.save(sd);
            }

            case "WL" -> {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(hierarchyId);
                if (wlOpt.isEmpty()) return "WhiteLabel not found";
                WhiteLabels wl = wlOpt.get();
                if (!"PENDING".equalsIgnoreCase(wl.getStatus())) return "Already processed";
                if (isApproved) {
                    wl.setStatus("APPROVED");
                    wl.setRemarks(remarks);
                    wl.setApprovedBy(loginId);
                    wl.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, wl.getWhiteLabelId(), wl.getMobileNumber(), wl.getEmailId(), wl.getTitle(),wl.getParentAccount(),wl.getCreatedBy(),loginId);
                } else {
                    wl.setStatus("REJECTED");
                    wl.setRejectionReason(remarks);
                }
                whiteLabelsRepository.save(wl);
            }
            case "ET" -> {
                Optional<Enterprises> etOpt = enterprisesRepository.findById(hierarchyId);
                if (etOpt.isEmpty()) return "Enterprise not found";
                Enterprises et = etOpt.get();
                if (!"PENDING".equalsIgnoreCase(et.getStatus())) return "Already processed";
                if (isApproved) {
                    et.setStatus("APPROVED");
                    et.setRemarks(remarks);
                    et.setApprovedBy(loginId);
                    et.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, et.getEnterpriseId(), et.getMobileNumber(), et.getEmailId(), et.getName(),et.getParentAccount(),et.getCreatedBy(),loginId);
                } else {
                    et.setStatus("REJECTED");
                    et.setRejectionReason(remarks);
                }
                enterprisesRepository.save(et);
            }
            case "AD", "IM" -> {
                Optional<Admin> adOpt = adminRepository.findById(hierarchyId);
                if (adOpt.isEmpty()) return "Admin not found";
                Admin ad = adOpt.get();
                if (!"PENDING".equalsIgnoreCase(ad.getStatus())) return "Already processed";
                if (isApproved) {
                    ad.setStatus("APPROVED");
                    ad.setRejectionReason("-");
                    ad.setRemarks(remarks);
                    ad.setApprovedBy(loginId);
                    ad.setApprovedOn(LocalDateTime.now().withNano(0));                    response = createUserDetails(hierarchyType, ad.getId(), ad.getMobileNumber(), ad.getEmailId(), ad.getName(),"Admin",ad.getCreatedBy(),loginId);
                } else {
                    ad.setStatus("REJECTED");
                    ad.setRejectionReason(remarks);
                }
                adminRepository.save(ad);
            }
            default -> {
                return "Invalid hierarchy type";
            }
        }

        return isApproved ? "Hierarchy approved successfully " + response : "Hierarchy rejected successfully";
    }

    private String createUserDetails(String hierarchyType, String hierarchyId, String mobileNo, String email, String name,String parentAccount, String createdBy, String editedBy) {
        String randomPassword = PasswordGeneratorUtil.generateRandomPassword(8); // e.g., Ab1!xY2@
        String encodedPassword = passwordEncoder.encode(randomPassword);

        UserDetails user = new UserDetails();
        user.setLoginUserName(name);
        user.setHierarchyCode(hierarchyType);
        user.setHierarchyId(hierarchyId);
        user.setMobileNumber(mobileNo);
        user.setEmailId(email);
        user.setLoginPassword(encodedPassword);
        user.setUserStatus("Active");
        user.setCreatedBy(createdBy);
        user.setEditedBy(editedBy);
        user.setEditedDate(LocalDate.now());
        user.setEditedTime(LocalTime.now());
        user.setParentAccount(parentAccount);

        // Only for RT, set hardcoded role_id and save subRoleId in ImSubRole
        if ("RT".equalsIgnoreCase(hierarchyType)) {
            user.setRole_id("11111");
            String subRoleIdStr = "2.1,2.2,2.3,2.4,3.1,3.2,3.3,3.4,4.1,4.2,4.3,5.1,5.2,5.3,5.4,5.5,5.6,5.7,5.8";

            //  Save subRoleId to ImSubRole table
            ImSubRole imSubRole = new ImSubRole();
            imSubRole.setMobileNumber(mobileNo);
            imSubRole.setSubRoleId(subRoleIdStr);
            imSubRoleRepository.save(imSubRole);
        }

        userDetailsRepository.save(user);

        String fromId = resolveFromEmail(hierarchyType, hierarchyId);

        EmailCredentialsMaster credentials = emailCredentialsRepository
                .findByWhitelabelId(fromId)
                .orElseGet(() -> emailCredentialsRepository
                        .findByWhitelabelId("IM-AD-001")
                        .orElseThrow(() -> new RuntimeException("Default email credentials not found")));

        String fromEmail = credentials.getEmail();
        String fromPassword = credentials.getPassword();

        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("mobile", mobileNo);
        vars.put("password", randomPassword);

        String emailBody = emailService.buildEmailFromDB(EmailType.ONBOARD_USER, vars);

        emailService.sendSimpleEmail(
                credentials,
                fromEmail,
                fromPassword,
                email,
                "Welcome to NFT – Your Login Credentials",
                emailBody
        );

        log.info("User created for hierarchy: " + hierarchyType + ", mobile: " + mobileNo);
        log.info("SMS (mock) sent to mobile: " + mobileNo + " with password: " + randomPassword);

        //smsService.sendSms(mobile, "Your account has been created. Password: " + randomPassword);
        // For testing: return plain password (REMOVE THIS IN PRODUCTION!)
        return randomPassword;
    }

    public static class PendingRecordMapper {

        public static GenericPendingRecordDto fromRetailer(Retailer r) {
            return new GenericPendingRecordDto("RETAILER", r.getRetailerId(), r.getName(), r.getStatus(),
                    r.getCreatedBy(), toStringOrNull(r.getCreatedOn()), r.getMobileNumber(), r.getEmailId(), r.getParentAccount(), r.getRejectionReason(), r.getEditedBy(), toStringOrNull(r.getLastEditedOn()), r.getApprovedBy(), toStringOrNull(r.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromDistributor(Distributor d) {
            return new GenericPendingRecordDto("DISTRIBUTOR", d.getDistributorId(), d.getName(), d.getStatus(),
                    d.getCreatedBy(), toStringOrNull(d.getCreatedOn()), d.getMobileNumber(), d.getEmailId(), d.getParentAccount(), d.getRejectionReason(), d.getEditedBy(), toStringOrNull(d.getLastEditedOn()), d.getApprovedBy(), toStringOrNull(d.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromPartner(Partner p) {
            return new GenericPendingRecordDto("PARTNER", p.getPartnerId(), p.getName(), p.getStatus(),
                    p.getCreatedBy(), toStringOrNull(p.getCreatedOn()), p.getMobileNumber(), p.getEmailId(), p.getParentAccount(), p.getRejectionReason(), p.getEditedBy(), toStringOrNull(p.getLastEditedOn()), p.getApprovedBy(), toStringOrNull(p.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromSuperDistributor(SuperDistributor s) {
            return new GenericPendingRecordDto("SUPER_DISTRIBUTOR", s.getSuperDistributorId(), s.getName(), s.getStatus(),
                    s.getCreatedBy(), toStringOrNull(s.getCreatedOn()), s.getMobileNumber(), s.getEmailId(), s.getParentAccount(), s.getRejectionReason(), s.getEditedBy(), toStringOrNull(s.getLastEditedOn()), s.getApprovedBy(), toStringOrNull(s.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromWhiteLabel(WhiteLabels w) {
            return new GenericPendingRecordDto("WHITE_LABEL", w.getWhiteLabelId(), w.getName(), w.getStatus(),
                    w.getCreatedBy(), toStringOrNull(w.getCreatedOn()), w.getMobileNumber(), w.getEmailId(), w.getParentAccount(), w.getRejectionReason(), w.getEditedBy(), toStringOrNull(w.getEditedOn()), w.getApprovedBy(), toStringOrNull(w.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromEnterprise(Enterprises e) {
            return new GenericPendingRecordDto("ENTERPRISE", e.getEnterpriseId(), e.getName(), e.getStatus(),
                    e.getCreatedBy(), toStringOrNull(e.getCreatedOn()), e.getMobileNumber(), e.getEmailId(), e.getParentAccount(), e.getRejectionReason(), e.getEditedBy(), toStringOrNull(e.getEditedOn()), e.getApprovedBy(), toStringOrNull(e.getApprovedOn()));
        }

        public static GenericPendingRecordDto fromAdmin(Admin a) {
            return new GenericPendingRecordDto("ADMIN", a.getId(), a.getName(), a.getStatus(),
                    a.getCreatedBy(), toStringOrNull(a.getCreatedOn()), a.getMobileNumber(), a.getEmailId(), null, null, null, null, null, null);
        }

        private static String toStringOrNull(LocalDateTime dt) {
            return dt != null ? dt.toString() : null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GenericPendingRecordDto getRecordById(String id) {
        // Try each repository one by one
        Optional<Retailer> retailer = retailerRepository.findById(id);
        if (retailer.isPresent()) {
            return PendingRecordMapper.fromRetailer(retailer.get());
        }

        Optional<Distributor> distributor = distributorRepository.findById(id);
        if (distributor.isPresent()) {
            return PendingRecordMapper.fromDistributor(distributor.get());
        }

        Optional<Partner> partner = partnerRepository.findById(id);
        if (partner.isPresent()) {
            return PendingRecordMapper.fromPartner(partner.get());
        }

        Optional<SuperDistributor> sd = superDistributorRepository.findById(id);
        if (sd.isPresent()) {
            return PendingRecordMapper.fromSuperDistributor(sd.get());
        }

        Optional<WhiteLabels> wl = whiteLabelsRepository.findById(id);
        if (wl.isPresent()) {
            return PendingRecordMapper.fromWhiteLabel(wl.get());
        }

        Optional<Enterprises> ent = enterprisesRepository.findById(id);
        if (ent.isPresent()) {
            return PendingRecordMapper.fromEnterprise(ent.get());
        }

        Optional<Admin> admin = adminRepository.findById(id);
        if (admin.isPresent()) {
            return PendingRecordMapper.fromAdmin(admin.get());
        }

        // If not found in any
        throw new RuntimeException("No record found for ID: " + id);
    }


    @Override
    @Transactional
    public String changeRejectedUserToPending(ChangeStatusToPendingRequest changeStatusToPending) {
        LocalDateTime imRequestDateTime = LocalDateTime.now();
        String requestPayload = "changeRejectedUserToPending: userId=" + changeStatusToPending.getUserId();
        String responsePayload = "";

        try {
            String type = commonMethods.extractHierarchyType(changeStatusToPending.getUserId());

            responsePayload = switch (type) {
                case "EN" -> updateEnterpriseStatus(changeStatusToPending);
                case "WL" -> updateWhiteLabelStatus(changeStatusToPending);
                case "SD" -> updateSuperDistributorStatus(changeStatusToPending);
                case "PT" -> updatePartnerStatus(changeStatusToPending);
                case "DT" -> updateDistributorStatus(changeStatusToPending);
                case "RT" -> updateRetailerStatus(changeStatusToPending);
                default -> throw new RuntimeException("Invalid hierarchy type: " + type);
            };

            return responsePayload;
        } catch (Exception ex) {
            responsePayload = "ERROR: " + ex.getMessage();
            throw ex;
        } finally {
            LocalDateTime imResponseDateTime = LocalDateTime.now();

            try {
                commonFunctions.saveImLogs(requestPayload, imRequestDateTime, null, responsePayload, imResponseDateTime);
            } catch (Exception logEx) {
                log.error("Failed to save IM logs for changeRejectedUserToPending userId={} : {}", changeStatusToPending.getUserId(), logEx.getMessage(), logEx);
            }
        }
    }


    private String updateEnterpriseStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        Enterprises ent = enterprisesRepository.findByEnterpriseId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("Enterprise not found"));

        if (!"REJECTED".equalsIgnoreCase(ent.getStatus())) {
            return "User is not in REJECTED state.";
        }

        ent.setStatus("PENDING");
        ent.setRemarks(changeStatusToPending.getRemarks());
        enterprisesRepository.save(ent);

        return "Enterprise status updated to PENDING successfully.";
    }

    private String updateWhiteLabelStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        WhiteLabels wl = whiteLabelsRepository.findByWhiteLabelId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("White Label not found"));

        if (!"REJECTED".equalsIgnoreCase(wl.getStatus())) {
            return "User is not in REJECTED state.";
        }

        wl.setStatus("PENDING");
        wl.setRemarks(changeStatusToPending.getRemarks());
        whiteLabelsRepository.save(wl);

        return "White Label status updated to PENDING successfully.";
    }

    private String updateSuperDistributorStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        SuperDistributor sd = superDistributorRepository.findBySuperDistributorId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("Super Distributor not found"));

        if (!"REJECTED".equalsIgnoreCase(sd.getStatus())) {
            return "User is not in REJECTED state.";
        }

        sd.setStatus("PENDING");
        sd.setRemarks(changeStatusToPending.getRemarks());
        superDistributorRepository.save(sd);

        return "Super Distributor status updated to PENDING successfully.";
    }

    private String updatePartnerStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        Partner pt = partnerRepository.findByPartnerId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        if (!"REJECTED".equalsIgnoreCase(pt.getStatus())) {
            return "User is not in REJECTED state.";
        }

        pt.setStatus("PENDING");
        pt.setRemarks(changeStatusToPending.getRemarks());
        partnerRepository.save(pt);

        return "Partner status updated to PENDING successfully.";
    }

    private String updateDistributorStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        Distributor sd = distributorRepository.findByDistributorId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("Distributor not found"));

        if (!"REJECTED".equalsIgnoreCase(sd.getStatus())) {
            return "User is not in REJECTED state.";
        }

        sd.setStatus("PENDING");
        sd.setRemarks(changeStatusToPending.getRemarks());
        distributorRepository.save(sd);

        return "Distributor status updated to PENDING successfully.";
    }

    private String updateRetailerStatus(ChangeStatusToPendingRequest changeStatusToPending) {
        Retailer rt = retailerRepository.findByRetailerId(changeStatusToPending.getUserId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        if (!"REJECTED".equalsIgnoreCase(rt.getStatus())) {
            return "User is not in REJECTED state.";
        }

        rt.setStatus("PENDING");
        rt.setRemarks(changeStatusToPending.getRemarks());
        retailerRepository.save(rt);

        return "Retailer status updated to PENDING successfully.";
    }


    @Override
    @Transactional
    public String moveToPending(String hierarchyId) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);

        switch (hierarchyType) {

            case "RT" -> {
                Retailer rt = retailerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Retailer not found"));

                if (!"REJECTED".equalsIgnoreCase(rt.getStatus()))
                    return "Only REJECTED retailers can be moved to PENDING";

                rt.setStatus("PENDING");
                rt.setRejectionReason(null);
                rt.setRemarks("Re-submitted for approval");
                retailerRepository.save(rt);
            }

            case "DT" -> {
                Distributor dt = distributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Distributor not found"));

                if (!"REJECTED".equalsIgnoreCase(dt.getStatus()))
                    return "Only REJECTED distributors can be moved to PENDING";

                dt.setStatus("PENDING");
                dt.setRejectionReason(null);
                dt.setRemarks("Re-submitted for approval");
                distributorRepository.save(dt);
            }

            case "PT" -> {
                Partner pt = partnerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Partner not found"));

                if (!"REJECTED".equalsIgnoreCase(pt.getStatus()))
                    return "Only REJECTED partners can be moved to PENDING";

                pt.setStatus("PENDING");
                pt.setRejectionReason(null);
                pt.setRemarks("Re-submitted for approval");
                partnerRepository.save(pt);
            }

            case "SD" -> {
                SuperDistributor sd = superDistributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Super Distributor not found"));

                if (!"REJECTED".equalsIgnoreCase(sd.getStatus()))
                    return "Only REJECTED super distributors can be moved to PENDING";

                sd.setStatus("PENDING");
                sd.setRejectionReason(null);
                sd.setRemarks("Re-submitted for approval");
                superDistributorRepository.save(sd);
            }

            case "WL" -> {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("WhiteLabel not found"));

                if (!"REJECTED".equalsIgnoreCase(wl.getStatus()))
                    return "Only REJECTED whitelabels can be moved to PENDING";

                wl.setStatus("PENDING");
                wl.setRejectionReason(null);
                wl.setRemarks("Re-submitted for approval");
                whiteLabelsRepository.save(wl);
            }

            case "ET" -> {
                Enterprises et = enterprisesRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Enterprise not found"));

                if (!"REJECTED".equalsIgnoreCase(et.getStatus()))
                    return "Only REJECTED enterprises can be moved to PENDING";

                et.setStatus("PENDING");
                et.setRejectionReason(null);
                et.setRemarks("Re-submitted for approval");
                enterprisesRepository.save(et);
            }

            case "AD", "IM" -> {
                Admin ad = adminRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Admin not found"));

                if (!"REJECTED".equalsIgnoreCase(ad.getStatus()))
                    return "Only REJECTED admins can be moved to PENDING";

                ad.setStatus("PENDING");
                ad.setRejectionReason(null);
                ad.setRemarks("Re-submitted for approval");
                adminRepository.save(ad);
            }

            default -> {
                return "Invalid hierarchy type";
            }
        }

        return "Moved to pending successfully";
    }

    private String resolveFromEmail(String hierarchyType, String hierarchyId) {

        switch (hierarchyType) {

            case "RT" -> {
                Retailer rt = retailerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Retailer not found"));

                String parentId = rt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "DT" -> {
                Distributor dt = distributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Distributor not found"));

                String parentId = dt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "PT" -> {
                Partner pt = partnerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Partner not found"));

                String parentId = pt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "SD" -> {
                SuperDistributor sd = superDistributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("SuperDistributor not found"));

                String parentId = sd.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "WL" -> {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("WhiteLabel not found"));

                if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                    return wl.getWhiteLabelId();
//                    return wl.getEmailId(); //
                }

                String parentId = wl.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "ET" -> {
                Enterprises et = enterprisesRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Enterprise not found"));

                String parentId = et.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "AD", "IM" -> {
                Admin ad = adminRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Admin not found"));

                return ad.getId();
//                return ad.getEmailId(); //
            }

            default -> throw new RuntimeException("Invalid hierarchy type: " + hierarchyType);
        }
    }


    private String resolveAccessRoot(
            String loginHierarchyId,
            String loginHierarchyType
    ) {

        if (!CommonMethods.hasText(loginHierarchyId)) {
            throw new IllegalStateException(
                    "Login hierarchy ID is missing"
            );
        }

        if (!CommonMethods.hasText(loginHierarchyType)) {
            throw new IllegalStateException(
                    "Login hierarchy type is missing for: "
                            + loginHierarchyId
            );
        }

        String normalizedLoginType =
                loginHierarchyType
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String expectedParentType =
                DELEGATED_PARENT_TYPES.get(
                        normalizedLoginType
                );

        /*
         * This is a normal business hierarchy account:
         *
         * AD, WL, SD, PT, DT, RT, ET, etc.
         *
         * It uses its own hierarchy ID as the root.
         */
        if (expectedParentType == null) {

            log.info(
                    "Normal hierarchy account detected | "
                            + "loginHierarchyId={} | "
                            + "loginHierarchyType={} | "
                            + "accessRoot={}",
                    loginHierarchyId,
                    normalizedLoginType,
                    loginHierarchyId
            );

            return loginHierarchyId;
        }

        /*
         * Delegated account:
         *
         * AU   -> parent AD
         * WLAU -> parent WL
         */
        String parentHierarchyId =
                hierarchyAccessValidator
                        .findParentHierarchy(
                                loginHierarchyId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Parent hierarchy not found for delegated user: "
                                                + loginHierarchyId
                                )
                        );

        if (!CommonMethods.hasText(parentHierarchyId)
                || "-".equals(parentHierarchyId.trim())) {

            throw new IllegalStateException(
                    "Invalid parent hierarchy for delegated user: "
                            + loginHierarchyId
            );
        }

        String actualParentType =
                commonMethods.extractHierarchyType(
                        parentHierarchyId
                );

        if (!CommonMethods.hasText(actualParentType)) {
            throw new IllegalStateException(
                    "Unable to determine parent hierarchy type. "
                            + "Login hierarchy: "
                            + loginHierarchyId
                            + ", parent hierarchy: "
                            + parentHierarchyId
            );
        }

        actualParentType =
                actualParentType
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!expectedParentType.equals(actualParentType)) {

            throw new IllegalStateException(
                    "Invalid delegated hierarchy relationship. "
                            + "Login hierarchy: "
                            + loginHierarchyId
                            + ", login type: "
                            + normalizedLoginType
                            + ", parent hierarchy: "
                            + parentHierarchyId
                            + ", expected parent type: "
                            + expectedParentType
                            + ", actual parent type: "
                            + actualParentType
            );
        }

        log.info(
                "Delegated hierarchy authority resolved | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "parentHierarchyId={} | "
                        + "parentHierarchyType={}",
                loginHierarchyId,
                normalizedLoginType,
                parentHierarchyId,
                actualParentType
        );

        return parentHierarchyId;
    }

    private <T> List<T> safeFutureResult(
            CompletableFuture<List<T>> future,
            String entityName
    ) {

        try {

            List<T> result =
                    future.join();

            return result != null
                    ? result
                    : Collections.emptyList();

        } catch (CompletionException ex) {

            log.error(
                    "Failed to fetch {}",
                    entityName,
                    ex
            );

            throw new IllegalStateException(
                    "Failed to fetch " + entityName,
                    ex.getCause() != null
                            ? ex.getCause()
                            : ex
            );
        }
    }

    private boolean containsIgnoreCase(
            String actualValue,
            String filterValue
    ) {

        if (!CommonMethods.hasText(filterValue)) {
            return true;
        }

        if (!CommonMethods.hasText(actualValue)) {
            return false;
        }

        return actualValue
                .toLowerCase(Locale.ROOT)
                .contains(
                        filterValue
                                .trim()
                                .toLowerCase(Locale.ROOT)
                );
    }

    private boolean matchesExactIgnoreCase(
            String actualValue,
            String filterValue
    ) {

        if (!CommonMethods.hasText(filterValue)) {
            return true;
        }

        return actualValue != null
                && actualValue.equalsIgnoreCase(
                filterValue.trim()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
