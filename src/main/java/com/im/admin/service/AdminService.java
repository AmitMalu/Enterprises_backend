package com.im.admin.service;

import com.im.admin.entities.Admin;
import java.util.List;

public interface AdminService {

    Admin saveAdmin(Admin admin);

    Admin getAdminById(String id);

    List<Admin> getAllAdmins(int page, int size);

    Admin updateAdmin(String id, Admin updatedAdmin);

    void deleteAdmin(String id);
}

