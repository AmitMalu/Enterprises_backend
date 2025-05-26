package com.im.admin.service.impl;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.PendingStatusResponseDto;
import com.im.admin.entities.*;
import com.im.admin.exception.NoPendingRecordsFoundException;
import com.im.admin.repositories.*;
import com.im.admin.service.OnboardingService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.PasswordGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional
    public PendingStatusResponseDto getAllRecordsByStatus(String status) {
        PendingStatusResponseDto response = new PendingStatusResponseDto();

        List<Retailer> retailers = retailerRepository.findByStatus(status);
        List<Distributor> distributors = distributorRepository.findByStatus(status);
        List<Partner> partners = partnerRepository.findByStatus(status);
        List<SuperDistributor> superDistributors = superDistributorRepository.findByStatus(status);
        List<WhiteLabels> whiteLabels = whiteLabelsRepository.findByStatus(status);
        List<Enterprises> enterprises = enterprisesRepository.findByStatus(status);
        List<Admin> admins = adminRepository.findByStatus(status);

        boolean hasAnyData =
                !retailers.isEmpty() ||
                        !distributors.isEmpty() ||
                        !partners.isEmpty() ||
                        !superDistributors.isEmpty() ||
                        !whiteLabels.isEmpty() ||
                        !enterprises.isEmpty() ||
                        !admins.isEmpty();

        if (!hasAnyData) {
            throw new NoPendingRecordsFoundException("No pending records found in any hierarchy.");
        }

        response.setRetailers(retailers);
        response.setDistributors(distributors);
        response.setPartners(partners);
        response.setSuperDistributors(superDistributors);
        response.setWhiteLabels(whiteLabels);
        response.setEnterprises(enterprises);
        response.setAdmins(admins);

        return response;
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
                    response = createUserDetails(hierarchyType, et.getEnterpriseId(), et.getMobileNumber(), et.getEmailId(), et.getCompanyName());
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

}
