package com.scs.admin.service;

import com.scs.admin.dto.StatusWiseUserFullDto;
import com.scs.admin.dto.UserDetailsResponseDTO;
import com.scs.admin.dto.ViewUserResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PaginatedResponse;
import com.scs.admin.response.UserSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    CommonResponse<UserDetailsResponseDTO> updateUserBlockStatus(String idOrMobile, boolean block);

    List<StatusWiseUserFullDto> getUsersByStatus(String upperCase);

    Page<UserSearchResponseDto> searchActiveRetailers(String keyword, Pageable pageable);

    Page<ViewUserResponseDto> getAllUsers(int page, int size, String keyword);
}
