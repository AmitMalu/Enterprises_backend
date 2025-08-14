package com.im.admin.service;

import com.im.admin.dto.HierarchyUpdateDTO;

import java.util.List;

public interface UserMappingService {
    List<?> remapUsers( String fromId, String toId);

    List<HierarchyUpdateDTO>  remapMultipleUsers(List<String> userId, String newParentId);

    List<HierarchyUpdateDTO> getUsersByType(String upperCase);
}
