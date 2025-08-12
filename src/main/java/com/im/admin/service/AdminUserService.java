package com.im.admin.service;

import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.AdminUser;
import org.springframework.data.domain.Page;

public interface AdminUserService {
    AdminUser createAdminUser( AdminUserRequestDTO adminUserRequestDTO);

    Page<AdminUser> getAllAdminUsers(int page, int size);

    AdminUser updateAdminUser(Long id, AdminUserRequestDTO dto);

    AdminUser getAdminUserById(Long id);
}
