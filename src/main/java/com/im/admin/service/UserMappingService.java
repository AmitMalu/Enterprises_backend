package com.im.admin.service;

import com.im.admin.dto.HierarchyUpdateDTO;

import java.util.List;

public interface UserMappingService {
    List<?> remapUsers( String fromId, String toId);

    HierarchyUpdateDTO remapSingleUser(String userId, String newParentId);

}
