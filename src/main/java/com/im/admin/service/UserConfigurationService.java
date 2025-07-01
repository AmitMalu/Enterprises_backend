package com.im.admin.service;

import com.im.admin.dto.UserConfigurationRequestDto;
import com.im.admin.entities.UserConfiguration;

import java.util.List;

public interface UserConfigurationService {
    List<UserConfiguration> addUserConfiguration(UserConfigurationRequestDto dto);
}
