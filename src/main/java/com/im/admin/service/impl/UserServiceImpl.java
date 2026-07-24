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
import com.im.admin.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private TokenUtil tokenUtil;

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
    @Transactional(readOnly = true)
    public Page<ViewUserResponseDto> getAllUsers(
            int page,
            int size,
            String keyword
    ) {

        log.info(
                "Fetching users | page={} | size={} | keyword={}",
                page,
                size,
                keyword
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

        final String normalizedKeyword =
                keyword == null || keyword.isBlank()
                        ? null
                        : keyword.trim();

        final String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalStateException(
                    "Login hierarchy ID is missing from token"
            );
        }

        final String loginType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        if (loginType == null || loginType.isBlank()) {
            throw new IllegalStateException(
                    "Unable to determine hierarchy type for: "
                            + loginHierarchyId
            );
        }

        final String effectiveHierarchyId =
                commonMethods.resolveEffectiveHierarchyId(
                        loginHierarchyId,
                        loginType
                );

        final Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdDate"
                        )
                );

        final Page<UserDetails> usersPage;

        if ("AD".equalsIgnoreCase(loginType)) {

            usersPage =
                    userDetailsRepository
                            .findAllUsersWithKeyword(
                                    normalizedKeyword,
                                    pageable
                            );

        } else {

            usersPage =
                    userDetailsRepository
                            .findAccessibleUsers(
                                    effectiveHierarchyId,
                                    normalizedKeyword,
                                    pageable
                            );
        }

        log.info(
                "User hierarchy access resolved | "
                        + "loginHierarchyId={} | "
                        + "loginType={} | "
                        + "effectiveHierarchyId={} | "
                        + "recordsFound={}",
                loginHierarchyId,
                loginType,
                effectiveHierarchyId,
                usersPage.getTotalElements()
        );

        Page<ViewUserResponseDto> responsePage =
                usersPage.map(user -> {

                    ViewUserResponseDto dto =
                            new ViewUserResponseDto();

                    dto.setMobileNumber(
                            user.getMobileNumber()
                    );

                    dto.setHierarchyId(
                            user.getHierarchyId()
                    );

                    dto.setHierarchyCode(
                            user.getHierarchyCode()
                    );

                    dto.setLoginUserName(
                            user.getLoginUserName()
                    );

                    dto.setUserLoginStatus(
                            user.getUserLoginStatus()
                    );

                    dto.setLastLoginDate(
                            user.getLastLoginDate()
                    );

                    dto.setLastLoginTime(
                            user.getLastLoginTime()
                    );

                    dto.setUserStatus(
                            user.getUserStatus()
                    );

                    dto.setEmailId(
                            user.getEmailId()
                    );

                    dto.setCreatedDate(
                            user.getCreatedDate()
                    );

                    dto.setCreatedTime(
                            user.getCreatedTime()
                    );

                    dto.setCreatedBy(
                            user.getCreatedBy()
                    );

                    dto.setEditedDate(
                            user.getEditedDate()
                    );

                    dto.setEditedTime(
                            user.getEditedTime()
                    );

                    dto.setEditedBy(
                            user.getEditedBy()
                    );

                    dto.setRoleId(
                            user.getRole_id()
                    );

                    dto.setIsBlocked(
                            user.getIsBlocked()
                    );

                    setUserAddress(
                            dto,
                            user.getHierarchyId()
                    );

                    return dto;
                });

        log.info(
                "Returning users | "
                        + "page={} | pageElements={} | totalElements={}",
                page,
                responsePage.getNumberOfElements(),
                responsePage.getTotalElements()
        );

        return responsePage;
    }

    private void setUserAddress(
            ViewUserResponseDto dto,
            String hierarchyId
    ) {

        if (hierarchyId == null || hierarchyId.isBlank()) {
            log.warn(
                    "Cannot resolve address because hierarchy ID is empty"
            );
            return;
        }

        final String hierarchyType =
                commonMethods.extractHierarchyType(
                        hierarchyId
                );

        if (hierarchyType == null
                || hierarchyType.isBlank()) {

            log.warn(
                    "Cannot determine hierarchy type for ID: {}",
                    hierarchyId
            );

            return;
        }

        log.debug(
                "Resolving address | hierarchyId={} | hierarchyType={}",
                hierarchyId,
                hierarchyType
        );

        switch (hierarchyType.toUpperCase()) {

            case "RT" ->
                    retailerRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            case "DT" ->
                    distributorRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            case "PT" ->
                    partnerRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            case "SD" ->
                    superDistributorRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            case "WL" ->
                    whiteLabelRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            case "ET" ->
                    enterprisesRepository
                            .findById(hierarchyId)
                            .ifPresent(entity ->
                                    setAddress(
                                            dto,
                                            entity.getAddress()
                                    )
                            );

            default ->
                    log.debug(
                            "Address lookup is not configured | "
                                    + "hierarchyId={} | hierarchyType={}",
                            hierarchyId,
                            hierarchyType
                    );
        }
    }

    private void setAddress(ViewUserResponseDto dto, Address address) {
        if (address != null) {
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setZipCode(address.getZipCode());
        }
    }

}
