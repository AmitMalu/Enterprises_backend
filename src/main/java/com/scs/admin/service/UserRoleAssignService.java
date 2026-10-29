package com.scs.admin.service;

import com.scs.admin.dto.AssignUserRoleRequestDTO;

public interface UserRoleAssignService {

    void assignUserRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO);

    void assignSubRole(AssignUserRoleRequestDTO assignUserRoleRequestDTO);
}
