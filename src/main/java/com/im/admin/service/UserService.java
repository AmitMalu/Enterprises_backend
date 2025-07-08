package com.im.admin.service;

import com.im.admin.dto.StatusWiseUserFullDto;

import java.util.List;

public interface UserService {
    String updateUserBlockStatus(String hierarchyId, boolean block);

    List<StatusWiseUserFullDto> getUsersByStatus(String upperCase);

}
