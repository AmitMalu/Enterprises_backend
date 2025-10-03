package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.entities.ImMaster;
import com.im.admin.entities.ImSubMaster;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MasterService {

    List<String> getAllMasterNames();

    List<ImSubMasterResponseDto> getSubMasterDetails(Integer  masterId);

    String addMaster(MasterRequestDto masterRequestDto);


    Page<DropdownResponseDto> getAllSubMaster(int page, int size, Integer imMasterId, String imMasterName, Integer imSubMasterId, String imSubMasterName,    String createdBy, LocalDate createdOn);

    Page<MasterResponseDto> getAllMasters(int page, int size, Integer imMasterId, String imMasterName, String createdBy,
                                          LocalDate createdOn);

    MasterResponseDto getMasterById(Integer id);

    String addSubMaster(SubMasterRequestDto dto);

    List<MasterResponseDto> getAllMasterDropdown();
}
