package com.im.admin.service;

import com.im.admin.dto.StatusWiseUserFullDto;
import com.im.admin.response.PaginatedResponse;
import com.im.admin.response.UserSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    String updateUserBlockStatus(String hierarchyId, boolean block);

    List<StatusWiseUserFullDto> getUsersByStatus(String upperCase);

    Page<UserSearchResponseDto> searchActiveRetailers(String keyword, Pageable pageable);

}
