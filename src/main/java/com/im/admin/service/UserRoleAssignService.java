package com.im.admin.service;

import com.im.admin.dto.AssignUserRoleRequestDTO;

public interface UserRoleAssignService {

    void assignUserRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO);

    void assignSubRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO);
}
