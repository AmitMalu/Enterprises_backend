package com.im.admin.service;

import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;

import java.util.List;

public interface HierarchyMasterService {

    List<HierarchyMasterDto> getAllNameAndCode();

    List<HierarchyResponseDto> getHierarchyDetails(String hierarchyCode);

}
