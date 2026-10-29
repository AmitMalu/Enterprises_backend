package com.scs.admin.service;

import com.scs.admin.dto.UserConfigurationRequestDto;
import com.scs.admin.dto.UserConfigurationResponseDto;
import com.scs.admin.entities.UserConfiguration;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserConfigurationService {
    List<UserConfiguration> addUserConfiguration(UserConfigurationRequestDto dto);



    UserConfiguration getUserConfigurationById(Integer id);

    UserConfiguration updateUserConfiguration(Integer id, UserConfigurationRequestDto dto);


    Page<UserConfiguration>  getAllUserConfigurations(int page, int size, String userId, String fullName, String email, String mobile, String state, String userRole, String parentAccount, Integer menuId, Integer subMenuId);
}
