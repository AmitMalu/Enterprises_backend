package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.entities.Admin;
import java.util.List;

public interface AdminService {

    Admin saveAdmin(Admin admin);

    Admin getAdminById(String id) throws JsonProcessingException;

    List<Admin> getAllAdmins(int page, int size) throws JsonProcessingException;

    Admin updateAdmin(String id, Admin updatedAdmin);

    void deleteAdmin(String id);
}

