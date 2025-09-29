package com.im.admin.service.impl;

import com.im.admin.dto.StatusWiseUserFullDto;
import com.im.admin.dto.UserDetailsResponseDTO;
import com.im.admin.dto.ViewUserResponseDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaginatedResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.UserService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    private PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public CommonResponse<UserDetailsResponseDTO> updateUserBlockStatus(String idOrMobile, boolean block) {
        log.info("Updating block status for user with hierarchyId or mobile: {}, block: {}", idOrMobile, block);

        UserDetails user = userDetailsRepository.findByHierarchyIdOrMobileNumber(idOrMobile);

        if (user == null) {
            log.warn("No user found with hierarchyId or mobile number: {}", idOrMobile);
            throw new RuntimeException("User with hierarchyId or mobile number " + idOrMobile + " not found.");
        }

        log.debug("Updating user: {} - current block status: {}", user.getHierarchyId(), user.getIsBlocked());
        user.setIsBlocked(block);
        user.setEditedDate(LocalDate.now());
        user.setEditedTime(LocalTime.now());

        userDetailsRepository.save(user);
        log.info("Saved updated block status for user: {}", user.getHierarchyId());

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

        String message = block ? "User blocked successfully." : "User unblocked successfully.";
        log.info("Returning response: {}", message);

        return CommonResponse.success(dto, message);
    }


    @Override
    public List<StatusWiseUserFullDto> getUsersByStatus(String status) {
        Logger logger = LoggerFactory.getLogger(getClass());
        logger.info("Fetching users by status: {}", status);

        List<StatusWiseUserFullDto> result = new ArrayList<>();

        // Process each hierarchy type
        processHierarchy(adminRepository.findByStatusIgnoreCase(status), result, "Admin", logger);
        processHierarchy(enterprisesRepository.findByStatusIgnoreCase(status), result, "Enterprise", logger);
        processHierarchy(whiteLabelRepository.findByStatusIgnoreCase(status), result, "WhiteLabel", logger);
        processHierarchy(superDistributorRepository.findByStatusIgnoreCase(status), result, "SuperDistributor", logger);
        processHierarchy(partnerRepository.findByStatusIgnoreCase(status), result, "Partner", logger);
        processHierarchy(distributorRepository.findByStatusIgnoreCase(status), result, "Distributor", logger);
        processHierarchy(retailerRepository.findByStatusIgnoreCase(status), result, "Retailer", logger);

        logger.info("Total users fetched with status '{}': {}", status, result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> void processHierarchy(List<T> entities, List<StatusWiseUserFullDto> result, String type, Logger logger) {
        for (T entity : entities) {
            StatusWiseUserFullDto dto = new StatusWiseUserFullDto();

            switch (type) {
                case "Admin" -> {
                    Admin ad = (Admin) entity;
                    dto.setAdminId(ad.getId());
                    dto.setName(ad.getName());
                    dto.setMobileNumber(ad.getMobileNumber());
                    dto.setEmailId(ad.getEmailId());
                    dto.setStatus(ad.getStatus());
                    dto.setIsBlocked(userDetailsRepository.findFirstByHierarchyId(ad.getId()).map(UserDetails::getIsBlocked).orElse(false));
                }
                case "Enterprise" -> {
                    Enterprises e = (Enterprises) entity;
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
                }
                case "WhiteLabel" -> {
                    WhiteLabels wl = (WhiteLabels) entity;
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
                }
                case "SuperDistributor" -> {
                    SuperDistributor sd = (SuperDistributor) entity;
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
                }
                case "Partner" -> {
                    Partner pt = (Partner) entity;
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
                }
                case "Distributor" -> {
                    Distributor dt = (Distributor) entity;
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
                }
                case "Retailer" -> {
                    Retailer rt = (Retailer) entity;
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
                }
            }

            logger.debug("Processed {} user: {}", type, dto.getName());
            result.add(dto);
        }
    }


    @Override
    public Page<UserSearchResponseDto> searchActiveRetailers(String keyword, Pageable pageable) {
        log.info("Searching active retailers with keyword: {}", keyword);

        Page<UserDetails> page = userDetailsRepository.searchActiveUsersByHierarchyId(keyword, pageable);
        log.debug("Found {} active users matching keyword '{}'", page.getTotalElements(), keyword);

        Page<UserSearchResponseDto> dtoPage = page.map(u -> {
            UserSearchResponseDto dto = new UserSearchResponseDto();
            dto.setMobileNumber(u.getMobileNumber());
            dto.setHierarchyId(u.getHierarchyId());
            dto.setHierarchyCode(u.getHierarchyCode());
            dto.setCreatedDate(u.getCreatedDate());
            dto.setLastLoginDate(u.getLastLoginDate());
            dto.setEmailId(u.getEmailId());
            return dto;
        });

        log.info("Returning {} search results for active retailers", dtoPage.getTotalElements());
        return dtoPage;
    }


    @Override
    public Page<ViewUserResponseDto> getAllUsers(int page, int size, String keyword) {
        log.info("Fetching users - page: {}, size: {}, keyword: {}", page, size, keyword);

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

                    String type = commonMethods.extractHierarchyType(user.getHierarchyId());
                    log.debug("Resolving address for user with hierarchyId: {}, type: {}", user.getHierarchyId(), type);

                    switch (type) {
                        case "RT" ->
                                retailerRepository.findById(user.getHierarchyId()).ifPresent(rt -> setAddress(dto, rt.getAddress()));
                        case "DT" ->
                                distributorRepository.findById(user.getHierarchyId()).ifPresent(dt -> setAddress(dto, dt.getAddress()));
                        case "PT" ->
                                partnerRepository.findById(user.getHierarchyId()).ifPresent(pt -> setAddress(dto, pt.getAddress()));
                        case "SD" ->
                                superDistributorRepository.findById(user.getHierarchyId()).ifPresent(sd -> setAddress(dto, sd.getAddress()));
                        case "WL" ->
                                whiteLabelRepository.findById(user.getHierarchyId()).ifPresent(wl -> setAddress(dto, wl.getAddress()));
                        case "ET" ->
                                enterprisesRepository.findById(user.getHierarchyId()).ifPresent(et -> setAddress(dto, et.getAddress()));
                        default -> log.warn("Unknown hierarchy type for ID: {}", user.getHierarchyId());
                    }

                    return dto;
                })
                .filter(dto -> keyword == null || keyword.isEmpty()
                        || (dto.getMobileNumber() != null && dto.getMobileNumber().contains(keyword))
                        || (dto.getHierarchyId() != null && dto.getHierarchyId().toLowerCase().contains(keyword.toLowerCase())))
                .toList();

        log.info("Returning {} users after filtering", dtoList.size());
        return new PageImpl<>(dtoList, pageable, usersPage.getTotalElements());
    }

    private void setAddress(ViewUserResponseDto dto, Address address) {
        if (address != null) {
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setZipCode(address.getZipCode());
        }
    }

}
