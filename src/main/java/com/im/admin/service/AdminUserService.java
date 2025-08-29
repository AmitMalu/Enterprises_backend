package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.AdminUser;
import org.springframework.data.domain.Page;

public interface AdminUserService {
    AdminUser createAdminUser( AdminUserRequestDTO adminUserRequestDTO) throws JsonProcessingException;

    Page<AdminUser> getAllAdminUsers(int page, int size) throws JsonProcessingException;

    AdminUser updateAdminUser(Long id, AdminUserRequestDTO dto) throws JsonProcessingException;

    AdminUser getAdminUserById(Long id) throws JsonProcessingException;
}
