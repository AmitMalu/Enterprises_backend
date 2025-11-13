package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.entities.*;
import com.im.admin.exception.NoPendingRecordsFoundException;
import com.im.admin.repositories.*;
import com.im.admin.request.ChangeStatusToPendingRequest;
import com.im.admin.response.GenericPendingResponseDto;
import com.im.admin.service.EmailService;
import com.im.admin.service.OnboardingService;
import com.im.admin.service.asyncMethods.AsyncPendingFetcher;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.PasswordGeneratorUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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


    @Override
    @Transactional(readOnly = true)
    public Page<GenericPendingRecordDto> getAllRecordsByStatus(String status, int page, int size,
                                                               String type,
                                                               String id,
                                                               String name,
                                                               String createdBy,
                                                               String createdOn,
                                                               String mobileNumber,
                                                               String emailId,
                                                               String parentAccount) {

        CompletableFuture<List<Retailer>> retailersFuture = asyncPendingFetcher.fetchRetailers(status);
        CompletableFuture<List<Distributor>> distributorsFuture = asyncPendingFetcher.fetchDistributors(status);
        CompletableFuture<List<Partner>> partnersFuture = asyncPendingFetcher.fetchPartners(status);
        CompletableFuture<List<SuperDistributor>> superDistributorsFuture = asyncPendingFetcher.fetchSuperDistributors(status);
        CompletableFuture<List<WhiteLabels>> whiteLabelsFuture = asyncPendingFetcher.fetchWhiteLabels(status);
        CompletableFuture<List<Enterprises>> enterprisesFuture = asyncPendingFetcher.fetchEnterprises(status);

        CompletableFuture.allOf(retailersFuture, distributorsFuture, partnersFuture, superDistributorsFuture,
                whiteLabelsFuture, enterprisesFuture).join();

        List<GenericPendingRecordDto> records = new ArrayList<>();
        records.addAll(retailersFuture.join().stream().map(PendingRecordMapper::fromRetailer).toList());
        records.addAll(distributorsFuture.join().stream().map(PendingRecordMapper::fromDistributor).toList());
        records.addAll(partnersFuture.join().stream().map(PendingRecordMapper::fromPartner).toList());
        records.addAll(superDistributorsFuture.join().stream().map(PendingRecordMapper::fromSuperDistributor).toList());
        records.addAll(whiteLabelsFuture.join().stream().map(PendingRecordMapper::fromWhiteLabel).toList());
        records.addAll(enterprisesFuture.join().stream().map(PendingRecordMapper::fromEnterprise).toList());

        if (records.isEmpty()) {
            throw new NoPendingRecordsFoundException("No " + status + " records found in any hierarchy.");
        }

        // Apply filters
        List<GenericPendingRecordDto> filteredRecords = records.stream()
                .filter(r -> status == null || status.isEmpty() || r.getStatus().equalsIgnoreCase(status))
                .filter(r -> type == null || type.isEmpty() || r.getType().toLowerCase().contains(type.toLowerCase()))
                .filter(r -> id == null || id.isEmpty() || r.getId().toLowerCase().contains(id.toLowerCase()))
                .filter(r -> name == null || name.isEmpty() || r.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(r -> createdBy == null || createdBy.isEmpty() || r.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase()))
                .filter(r -> createdOn == null || createdOn.isEmpty() || r.getCreatedOn().contains(createdOn))
                .filter(r -> mobileNumber == null || mobileNumber.isEmpty() || r.getMobileNumber().contains(mobileNumber))
                .filter(r -> emailId == null || emailId.isEmpty() || r.getEmailId().toLowerCase().contains(emailId.toLowerCase()))
                .filter(r -> parentAccount == null || parentAccount.isEmpty() || r.getParentAccount().toLowerCase().contains(parentAccount.toLowerCase()))
                //  sort by createdOn descending (latest first)
                .sorted((a, b) -> b.getCreatedOn().compareTo(a.getCreatedOn()))
                .toList();


        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredRecords.size());
        List<GenericPendingRecordDto> paginatedList = filteredRecords.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, filteredRecords.size());
    }

    @Override
    @Transactional
    public String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto) {

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
                    response = createUserDetails(hierarchyType, rt.getRetailerId(), rt.getMobileNumber(), rt.getEmailId(), rt.getName());
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
                    response = createUserDetails(hierarchyType, dt.getDistributorId(), dt.getMobileNumber(), dt.getEmailId(), dt.getName());
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
                    response = createUserDetails(hierarchyType, pt.getPartnerId(), pt.getMobileNumber(), pt.getEmailId(), pt.getName());
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
                    response = createUserDetails(hierarchyType, sd.getSuperDistributorId(), sd.getMobileNumber(), sd.getEmailId(), sd.getName());
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
                    response = createUserDetails(hierarchyType, wl.getWhiteLabelId(), wl.getMobileNumber(), wl.getEmailId(), wl.getTitle());
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
                    response = createUserDetails(hierarchyType, et.getEnterpriseId(), et.getMobileNumber(), et.getEmailId(), et.getName());
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
                    response = createUserDetails(hierarchyType, ad.getId(), ad.getMobileNumber(), ad.getEmailId(), ad.getName());
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

    private String createUserDetails(String hierarchyType, String hierarchyId, String mobileNo, String email, String name) {
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
        user.setCreatedBy("ADMIN");
        user.setEditedBy("ADMIN");
        user.setEditedDate(LocalDate.now());
        user.setEditedTime(LocalTime.now());

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

        //Send email with credentials
        String emailBody = emailService.buildWelcomeEmail(name, mobileNo, randomPassword);
        emailService.sendSimpleEmail(email, "Welcome to NFT – Your Login Credentials", emailBody);

        log.info("User created for hierarchy: " + hierarchyType + ", mobile: " + mobileNo);
        log.info("SMS (mock) sent to mobile: " + mobileNo + " with password: " + randomPassword);

        //smsService.sendSms(mobile, "Your account has been created. Password: " + randomPassword);
        // For testing: return plain password (REMOVE THIS IN PRODUCTION!)
        return randomPassword;
    }

    public class PendingRecordMapper {

        public static GenericPendingRecordDto fromRetailer(Retailer r) {
            return new GenericPendingRecordDto("RETAILER", r.getRetailerId(), r.getName(), r.getStatus(),
                    r.getCreatedBy(), toStringOrNull(r.getCreatedOn()), r.getMobileNumber(), r.getEmailId(), r.getParentAccount());
        }

        public static GenericPendingRecordDto fromDistributor(Distributor d) {
            return new GenericPendingRecordDto("DISTRIBUTOR", d.getDistributorId(), d.getName(), d.getStatus(),
                    d.getCreatedBy(), toStringOrNull(d.getCreatedOn()), d.getMobileNumber(), d.getEmailId(), d.getParentAccount());
        }

        public static GenericPendingRecordDto fromPartner(Partner p) {
            return new GenericPendingRecordDto("PARTNER", p.getPartnerId(), p.getName(), p.getStatus(),
                    p.getCreatedBy(), toStringOrNull(p.getCreatedOn()), p.getMobileNumber(), p.getEmailId(), p.getParentAccount());
        }

        public static GenericPendingRecordDto fromSuperDistributor(SuperDistributor s) {
            return new GenericPendingRecordDto("SUPER_DISTRIBUTOR", s.getSuperDistributorId(), s.getName(), s.getStatus(),
                    s.getCreatedBy(), toStringOrNull(s.getCreatedOn()), s.getMobileNumber(), s.getEmailId(), s.getParentAccount());
        }

        public static GenericPendingRecordDto fromWhiteLabel(WhiteLabels w) {
            return new GenericPendingRecordDto("WHITE_LABEL", w.getWhiteLabelId(), w.getName(), w.getStatus(),
                    w.getCreatedBy(), toStringOrNull(w.getCreatedOn()), w.getMobileNumber(), w.getEmailId(), w.getParentAccount());
        }

        public static GenericPendingRecordDto fromEnterprise(Enterprises e) {
            return new GenericPendingRecordDto("ENTERPRISE", e.getEnterpriseId(), e.getName(), e.getStatus(),
                    e.getCreatedBy(), toStringOrNull(e.getCreatedOn()), e.getMobileNumber(), e.getEmailId(), e.getParentAccount());
        }

        public static GenericPendingRecordDto fromAdmin(Admin a) {
            return new GenericPendingRecordDto("ADMIN", a.getId(), a.getName(), a.getStatus(),
                    a.getCreatedBy(), toStringOrNull(a.getCreatedOn()), a.getMobileNumber(), a.getEmailId(), null);
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

}
