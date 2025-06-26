package com.im.admin.service;

import com.im.admin.dto.DropdownRequestDto;
import com.im.admin.dto.DropdownResponseDto;
import com.im.admin.dto.ImSubMasterResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MasterService {

    List<String> getAllMasterNames();

    List<ImSubMasterResponseDto> getSubMasterDetails(Integer  masterId);

    String addDropdown(DropdownRequestDto dto);


    Page<DropdownResponseDto> getAllDropdowns(int page, int size, Integer imMasterId, String imMasterName, Integer imSubMasterId, String imSubMasterName);

}
