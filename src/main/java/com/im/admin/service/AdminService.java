package com.im.admin.service;

import com.im.admin.entities.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AdminService {

    Admin saveAdmin(Admin admin);

    Admin updateAdmin(Long id, Admin admin);

    void deleteAdmin(Long id);

    Page<Admin> getAllAdmins(Pageable pageable);

    Optional<Admin> getAdminById(Long id);
}

