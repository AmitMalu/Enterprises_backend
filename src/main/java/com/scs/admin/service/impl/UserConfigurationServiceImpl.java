package com.scs.admin.service.impl;

import com.scs.admin.dto.UserConfigurationRequestDto;
import com.scs.admin.dto.UserConfigurationResponseDto;
import com.scs.admin.entities.UserConfiguration;
import com.scs.admin.entities.WhiteLabels;
import com.scs.admin.repositories.UserConfigurationRepository;
import com.scs.admin.repositories.WhiteLabelRepository;
import com.scs.admin.service.UserConfigurationService;
import com.scs.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserConfigurationServiceImpl implements UserConfigurationService {

    @Autowired
    private UserConfigurationRepository userConfigurationRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Override
    public List<UserConfiguration> addUserConfiguration(UserConfigurationRequestDto dto) {

        // Join permissions into comma-separated string
        String permissionString = String.join(",", dto.getPermissions());

        UserConfiguration config = new UserConfiguration();
        config.setFullName(dto.getFullName());
        config.setEmail(dto.getEmail());
        config.setMobile(dto.getMobile());
        config.setState(dto.getState());
        config.setUserRole(dto.getUserRole());
        config.setParentAccount(dto.getParentAccount());
        config.setPermission(permissionString); // Correctly set

        // Save only one record
        return List.of(userConfigurationRepository.save(config));
    }


    @Override
    public UserConfiguration getUserConfigurationById(Integer id) {
        return userConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User Configuration not found with ID: " + id));
    }

    @Override
    public UserConfiguration updateUserConfiguration(Integer id, UserConfigurationRequestDto dto) {

        UserConfiguration config = userConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User configuration not found for id: " + id));

        config.setFullName(dto.getFullName());
        config.setEmail(dto.getEmail());
        config.setMobile(dto.getMobile());
        config.setState(dto.getState());
        config.setUserRole(dto.getUserRole());
        config.setParentAccount(dto.getParentAccount());

        String permissionString = String.join(",", dto.getPermissions());
        config.setPermission(permissionString);

        return userConfigurationRepository.save(config);
    }



    @Override
    public Page<UserConfiguration> getAllUserConfigurations(
            int page,
            int size,
            String userId,
            String fullName,
            String email,
            String mobile,
            String state,
            String userRole,
            String parentAccount,
            Integer menuId,
            Integer subMenuId
    ) {
        List<UserConfiguration> all = userConfigurationRepository.findAll();

        List<UserConfiguration> filtered = all.stream()
                .filter(u -> fullName == null || (u.getFullName() != null && u.getFullName().toLowerCase().contains(fullName.toLowerCase())))
                .filter(u -> email == null || (u.getEmail() != null && u.getEmail().toLowerCase().contains(email.toLowerCase())))
                .filter(u -> mobile == null || (u.getMobile() != null && u.getMobile().contains(mobile)))
                .filter(u -> state == null || (u.getState() != null && u.getState().equalsIgnoreCase(state)))
                .filter(u -> userRole == null || (u.getUserRole() != null && u.getUserRole().equalsIgnoreCase(userRole)))
                .filter(u -> parentAccount == null || (u.getParentAccount() != null && u.getParentAccount().equalsIgnoreCase(parentAccount)))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<UserConfiguration> paginated = filtered.subList(start, end);
        return new PageImpl<>(paginated, pageable, filtered.size());


    }


}
