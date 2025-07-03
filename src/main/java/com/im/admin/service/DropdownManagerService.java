package com.im.admin.service;

import com.im.admin.dto.ChildUserDto;

import java.util.List;

public interface DropdownManagerService {

    List<ChildUserDto> getChildUsers(String hierarchyId);
}
