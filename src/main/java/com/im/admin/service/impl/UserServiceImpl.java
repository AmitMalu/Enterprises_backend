package com.im.admin.service.impl;

import com.im.admin.dto.StatusWiseUserFullDto;
import com.im.admin.dto.UserDetailsResponseDTO;
import com.im.admin.dto.ViewUserResponseDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaginatedResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

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
    private SuperAdminRepository superAdminRepository;

    @Override
    public CommonResponse<List<UserDetailsResponseDTO>> updateUserBlockStatus(String idOrMobile, boolean block) {
        List<UserDetails> userList = userDetailsRepository.findByHierarchyIdOrMobileNumber(idOrMobile);

        if (userList.isEmpty()) {
            throw new RuntimeException("User with hierarchyId or mobile number " + idOrMobile + " not found.");
        }

        for (UserDetails user : userList) {
            user.setIsBlocked(block);
            user.setEditedDate(LocalDate.now());
            user.setEditedTime(LocalTime.now());
        }

        userDetailsRepository.saveAll(userList);

        // Convert entity list to DTO list
        List<UserDetailsResponseDTO> dtoList = userList.stream().map(user -> {
            UserDetailsResponseDTO dto = new UserDetailsResponseDTO();
            dto.setMobileNumber(user.getMobileNumber());
            dto.setHierarchyId(user.getHierarchyId());
            dto.setHierarchyCode(user.getHierarchyCode());
            dto.setLoginUserName(user.getLoginUserName());
            dto.setUserLoginStatus(user.getUserLoginStatus());
            dto.setLastLoginDate(user.getLastLoginDate());
            dto.setLastLoginTime(user.getLastLoginTime());
            dto.setUserStatus(user.getUserStatus());
            dto.setEmailId(user.getEmailId());
            dto.setCreatedDate(user.getCreatedDate());
            dto.setCreatedTime(user.getCreatedTime());
            dto.setCreatedBy(user.getCreatedBy());
            dto.setEditedDate(user.getEditedDate());
            dto.setEditedTime(user.getEditedTime());
            dto.setEditedBy(user.getEditedBy());
            dto.setRoleId(user.getRole_id());
            dto.setIsBlocked(user.getIsBlocked());
            return dto;
        }).toList();

        String message = block ? "User blocked successfully." : "User unblocked successfully.";
        return CommonResponse.success(dtoList, message);
    }



    @Override
    public List<StatusWiseUserFullDto> getUsersByStatus(String status) {
        List<StatusWiseUserFullDto> result = new ArrayList<>();

        // Admin
        adminRepository.findByStatusIgnoreCase(status).forEach(ad -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("Admin");
            dto.setAdminId(ad.getId());
            dto.setName(ad.getName());
            dto.setMobileNumber(ad.getMobileNumber());
            dto.setEmailId(ad.getEmailId());
            dto.setStatus(ad.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(ad.getId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // Enterprises
        enterprisesRepository.findByStatusIgnoreCase(status).forEach(e -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("Enterprises");
            dto.setEnterpriseId(e.getEnterpriseId());
            dto.setParentAccount(e.getParentAccount());
            dto.setAbbreviation(e.getAbbreviation());
            dto.setTitle(e.getTitle());
            dto.setName(e.getName());
            dto.setMobileNumber(e.getMobileNumber());
            dto.setEmailId(e.getEmailId());
            dto.setDob(e.getDob());
            dto.setStatus(e.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(e.getEnterpriseId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // WhiteLabel
        whiteLabelRepository.findByStatusIgnoreCase(status).forEach(wl -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("Whitelabel");
            dto.setWhiteLabelId(wl.getWhiteLabelId());
            dto.setParentAccount(wl.getParentAccount());
            dto.setAbbreviation(wl.getAbbreviation());
            dto.setTitle(wl.getTitle());
            dto.setName(wl.getName());
            dto.setMobileNumber(wl.getMobileNumber());
            dto.setEmailId(wl.getEmailId());
            dto.setDob(wl.getDob());
            dto.setStatus(wl.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(wl.getWhiteLabelId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // SuperDistributor
        superDistributorRepository.findByStatusIgnoreCase(status).forEach(sd -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("SuperDistributor");
            dto.setSuperDistributorId(sd.getSuperDistributorId());
            dto.setParentAccount(sd.getParentAccount());
            dto.setAbbreviation(sd.getAbbreviation());
            dto.setTitle(sd.getTitle());
            dto.setName(sd.getName());
            dto.setMobileNumber(sd.getMobileNumber());
            dto.setEmailId(sd.getEmailId());
            dto.setDob(sd.getDob());
            dto.setStatus(sd.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(sd.getSuperDistributorId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // Partner
        partnerRepository.findByStatusIgnoreCase(status).forEach(pt -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("Partner");
            dto.setPartnerId(pt.getPartnerId());
            dto.setParentAccount(pt.getParentAccount());
            dto.setAbbreviation(pt.getAbbreviation());
            dto.setTitle(pt.getTitle());
            dto.setName(pt.getName());
            dto.setMobileNumber(pt.getMobileNumber());
            dto.setEmailId(pt.getEmailId());
            dto.setDob(pt.getDob());
            dto.setStatus(pt.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(pt.getPartnerId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // Distributor
        distributorRepository.findByStatusIgnoreCase(status).forEach(dt -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
//            dto.setHierarchyType("Distributor");
            dto.setDistributorId(dt.getDistributorId());
            dto.setParentAccount(dt.getParentAccount());
            dto.setAbbreviation(dt.getAbbreviation());
            dto.setTitle(dt.getTitle());
            dto.setName(dt.getName());
            dto.setMobileNumber(dt.getMobileNumber());
            dto.setEmailId(dt.getEmailId());
            dto.setDob(dt.getDob());
            dto.setStatus(dt.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(dt.getDistributorId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        // Retailer
        retailerRepository.findByStatusIgnoreCase(status).forEach(rt -> {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();
            dto.setRetailerId(rt.getRetailerId());
            dto.setParentAccount(rt.getParentAccount());
            dto.setAbbreviation(rt.getAbbreviation());
            dto.setTitle(rt.getTitle());
            dto.setName(rt.getName());
            dto.setMobileNumber(rt.getMobileNumber());
            dto.setEmailId(rt.getEmailId());
            dto.setDob(rt.getDob());
            dto.setStatus(rt.getStatus());
            dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(rt.getRetailerId()).map(UserDetails::getIsBlocked).orElse(false));
            result.add(dto);
        });

        return result;
    }


    @Override
    public Page<UserSearchResponseDto> searchActiveRetailers(String keyword, Pageable pageable) {
        Page<UserDetails> page = userDetailsRepository.searchActiveUsersByHierarchyId(keyword, pageable);

        return page.map(u -> {
            UserSearchResponseDto dto = new UserSearchResponseDto();
            dto.setMobileNumber(u.getMobileNumber());
            dto.setHierarchyId(u.getHierarchyId());
            dto.setHierarchyCode(u.getHierarchyCode());
            dto.setCreatedDate(u.getCreatedDate());
            dto.setLastLoginDate(u.getLastLoginDate());
            dto.setEmailId(u.getEmailId());
            return dto;
        });
    }


    @Override
    public Page<ViewUserResponseDto> getAllUsers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<UserDetails> usersPage = userDetailsRepository.findAll(pageable);

        List<ViewUserResponseDto> dtoList = usersPage.stream()
                .map(user -> {
                    ViewUserResponseDto dto = new ViewUserResponseDto();
                    dto.setMobileNumber(user.getMobileNumber());
                    dto.setHierarchyId(user.getHierarchyId());
                    dto.setHierarchyCode(user.getHierarchyCode());
                    dto.setLoginUserName(user.getLoginUserName());
                    dto.setUserLoginStatus(user.getUserLoginStatus());
                    dto.setLastLoginDate(user.getLastLoginDate());
                    dto.setLastLoginTime(user.getLastLoginTime());
                    dto.setUserStatus(user.getUserStatus());
                    dto.setEmailId(user.getEmailId());
                    dto.setCreatedDate(user.getCreatedDate());
                    dto.setCreatedTime(user.getCreatedTime());
                    dto.setCreatedBy(user.getCreatedBy());
                    dto.setEditedDate(user.getEditedDate());
                    dto.setEditedTime(user.getEditedTime());
                    dto.setEditedBy(user.getEditedBy());
                    dto.setRoleId(user.getRole_id());
                    dto.setIsBlocked(user.getIsBlocked());

                    String type = extractHierarchyType(user.getHierarchyId());
                    switch (type) {
                        case "RT" -> retailerRepository.findById(user.getHierarchyId()).ifPresent(rt -> {
                            dto.setCity(rt.getAddress().getCity());
                            dto.setState(rt.getAddress().getState());
                            dto.setZipCode(rt.getAddress().getZipCode());
                        });
                        case "DT" -> distributorRepository.findById(user.getHierarchyId()).ifPresent(dt -> {
                            dto.setCity(dt.getAddress().getCity());
                            dto.setState(dt.getAddress().getState());
                            dto.setZipCode(dt.getAddress().getZipCode());
                        });
                        case "PT" -> partnerRepository.findById(user.getHierarchyId()).ifPresent(pt -> {
                            dto.setCity(pt.getAddress().getCity());
                            dto.setState(pt.getAddress().getState());
                            dto.setZipCode(pt.getAddress().getZipCode());
                        });
                        case "SD" -> superDistributorRepository.findById(user.getHierarchyId()).ifPresent(sd -> {
                            dto.setCity(sd.getAddress().getCity());
                            dto.setState(sd.getAddress().getState());
                            dto.setZipCode(sd.getAddress().getZipCode());
                        });
                        case "WL" -> whiteLabelRepository.findById(user.getHierarchyId()).ifPresent(wl -> {
                            dto.setCity(wl.getAddress().getCity());
                            dto.setState(wl.getAddress().getState());
                            dto.setZipCode(wl.getAddress().getZipCode());
                        });
                        case "ET" -> enterprisesRepository.findById(user.getHierarchyId()).ifPresent(et -> {
                            dto.setCity(et.getAddress().getCity());
                            dto.setState(et.getAddress().getState());
                            dto.setZipCode(et.getAddress().getZipCode());
                        });
                    }
                    return dto;
                })
                .filter(dto -> keyword == null || keyword.isEmpty()
                        || (dto.getMobileNumber() != null && dto.getMobileNumber().contains(keyword))
                        || (dto.getHierarchyId() != null
                        && dto.getHierarchyId().toLowerCase().contains(keyword.toLowerCase()))
                )
                .toList();

        return new PageImpl<>(dtoList, pageable, usersPage.getTotalElements());
    }



    private String extractHierarchyType(String hierarchyId) {
        return hierarchyId.split("-")[1];
    }



}
