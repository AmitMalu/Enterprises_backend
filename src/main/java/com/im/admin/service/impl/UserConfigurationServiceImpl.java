package com.im.admin.service.impl;

import com.im.admin.dto.UserConfigurationRequestDto;
import com.im.admin.entities.UserConfiguration;
import com.im.admin.entities.WhiteLabels;
import com.im.admin.repositories.UserConfigurationRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.service.UserConfigurationService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserConfigurationServiceImpl implements UserConfigurationService {

    @Autowired
    private UserConfigurationRepository userConfigurationRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Override
    public  List<UserConfiguration>  addUserConfiguration(UserConfigurationRequestDto dto) {

        String hierarchyId = dto.getHierarchyId();
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        Optional<WhiteLabels> WlOpt = whiteLabelRepository.findById(hierarchyId);
        if(WlOpt.isEmpty()){
            throw new RuntimeException("white label not found");
        }
        WhiteLabels whiteLabels = WlOpt.get();

        List<UserConfiguration> records = dto.getPermissions().stream().map(p -> {
            UserConfiguration config = new UserConfiguration();
            config.setUserId(dto.getUserId());
            config.setFullName(dto.getFullName());
            config.setEmail(dto.getEmail());
            config.setMobile(dto.getMobile());
            config.setState(dto.getState());
            config.setUserRole(dto.getUserRole());
            config.setMenuId(p.getMenuId());
            config.setSubMenuId(p.getSubMenuId());
            return config;
        }).collect(Collectors.toList());

        List<UserConfiguration> userConfigurations = userConfigurationRepository.saveAll(records);
        return userConfigurations;
    }
}
