package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.AdminUserRequestDTO;
import com.scs.admin.entities.AdminUser;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AdminUserService {
    AdminUser createAdminUser( AdminUserRequestDTO adminUserRequestDTO) throws JsonProcessingException;

    Page<AdminUser> getAllAdminUsers(int page, int size,
                                     String id,
                                     String name,
                                     String mobileNumber,
                                     String username,
                                     String emailId,
                                     Boolean status,
                                     LocalDate dob,
                                     LocalDate createdOn,
                                     String createdBy,
                                     LocalDate lastEditedOn,
                                     String editedBy) throws JsonProcessingException;


    AdminUser updateAdminUser(String id, AdminUserRequestDTO dto) throws JsonProcessingException;

    AdminUser getAdminUserById(String  id) throws JsonProcessingException;
}
