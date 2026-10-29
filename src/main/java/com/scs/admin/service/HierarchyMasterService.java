package com.scs.admin.service;

import com.scs.admin.dto.HierarchyMasterDto;
import com.scs.admin.dto.HierarchyResponseDto;

import java.util.List;

public interface HierarchyMasterService {

    List<HierarchyMasterDto> getAllNameAndCode();

    List<HierarchyResponseDto> getHierarchyDetails(String hierarchyCode);

}
