package com.im.admin.service;

import com.im.admin.dto.ChildUserDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DropdownManagerService {


    Page<ChildUserDto> getChildUsers(String hierarchyId, int page, int size, String userId, String parentAccount, String abbreviation, String title, String name, String mobileNumber, String emailId, String dob, String status, Boolean isBlocked);
}
