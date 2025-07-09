package com.im.admin.service.impl;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.entities.*;
import com.im.admin.exception.NoPendingRecordsFoundException;
import com.im.admin.repositories.*;
import com.im.admin.response.GenericPendingResponseDto;
import com.im.admin.service.OnboardingService;
import com.im.admin.service.asyncMethods.AsyncPendingFetcher;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.PasswordGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Override
    @Transactional(readOnly = true)
    public GenericPendingResponseDto getAllRecordsByStatus(String status) {

        CompletableFuture<List<Retailer>> retailersFuture = asyncPendingFetcher.fetchRetailers(status);
        CompletableFuture<List<Distributor>> distributorsFuture = asyncPendingFetcher.fetchDistributors(status);
        CompletableFuture<List<Partner>> partnersFuture = asyncPendingFetcher.fetchPartners(status);
        CompletableFuture<List<SuperDistributor>> superDistributorsFuture = asyncPendingFetcher.fetchSuperDistributors(status);
        CompletableFuture<List<WhiteLabels>> whiteLabelsFuture = asyncPendingFetcher.fetchWhiteLabels(status);
        CompletableFuture<List<Enterprises>> enterprisesFuture = asyncPendingFetcher.fetchEnterprises(status);
        CompletableFuture<List<Admin>> adminsFuture = asyncPendingFetcher.fetchAdmins(status);

        // Wait for all to complete
        CompletableFuture.allOf(retailersFuture, distributorsFuture, partnersFuture, superDistributorsFuture,
                whiteLabelsFuture, enterprisesFuture, adminsFuture).join();

        List<GenericPendingRecordDto> records = new ArrayList<>();
        records.addAll(retailersFuture.join().stream().map(PendingRecordMapper::fromRetailer).toList());
        records.addAll(distributorsFuture.join().stream().map(PendingRecordMapper::fromDistributor).toList());
        records.addAll(partnersFuture.join().stream().map(PendingRecordMapper::fromPartner).toList());
        records.addAll(superDistributorsFuture.join().stream().map(PendingRecordMapper::fromSuperDistributor).toList());
        records.addAll(whiteLabelsFuture.join().stream().map(PendingRecordMapper::fromWhiteLabel).toList());
        records.addAll(enterprisesFuture.join().stream().map(PendingRecordMapper::fromEnterprise).toList());
        records.addAll(adminsFuture.join().stream().map(PendingRecordMapper::fromAdmin).toList());

        if (records.isEmpty()) {
            throw new NoPendingRecordsFoundException("No pending records found in any hierarchy.");
        }

        return new GenericPendingResponseDto(records);
    }



    @Override
    @Transactional
    public String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto) {

        String response = null;
        String hierarchyId = hierarchyRequestDto.getHierarchyId();
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        boolean isApproved = hierarchyRequestDto.getIsApproved();
        String rejectionReason = hierarchyRequestDto.getRejectionReason();

        switch (hierarchyType) {
            case "RT" -> {
                Optional<Retailer> rtOpt = retailerRepository.findById(hierarchyId);
                if(rtOpt.isEmpty()) return "Retailer not found";
                Retailer rt = rtOpt.get();
                if (!"PENDING".equalsIgnoreCase(rt.getStatus())) return "Already processed";
                if (isApproved) {
                    rt.setStatus("APPROVED");
                    response = createUserDetails(hierarchyType, rt.getRetailerId(), rt.getMobileNumber(), rt.getEmailId(), rt.getName());
                } else {
                    rt.setStatus("REJECTED");
                    rt.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, dt.getDistributorId(), dt.getMobileNumber(), dt.getEmailId(), dt.getName());
                } else {
                    dt.setStatus("REJECTED");
                    dt.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, pt.getPartnerId(), pt.getMobileNumber(), pt.getEmailId(), pt.getName());
                } else {
                    pt.setStatus("REJECTED");
                    pt.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, sd.getSuperDistributorId(), sd.getMobileNumber(), sd.getEmailId(), sd.getName());
                } else {
                    sd.setStatus("REJECTED");
                    sd.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, wl.getWhiteLabelId(), wl.getMobileNumber(), wl.getEmailId(), wl.getTitle());
                } else {
                    wl.setStatus("REJECTED");
                    wl.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, et.getEnterpriseId(), et.getMobileNumber(), et.getEmailId(), et.getName());
                } else {
                    et.setStatus("REJECTED");
                    et.setRejectionReason(rejectionReason);
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
                    response = createUserDetails(hierarchyType, ad.getId(), ad.getMobileNumber(), ad.getEmailId(), ad.getName());
                } else {
                    ad.setStatus("REJECTED");
                    ad.setRejectionReason(rejectionReason);
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
        userDetailsRepository.save(user);

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


}
