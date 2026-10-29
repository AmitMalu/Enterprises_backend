package com.scs.admin.service;

import com.scs.admin.dto.*;
import com.scs.admin.entities.ScsMaster;
import com.scs.admin.entities.ScsSubMaster;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MasterService {

    List<String> getAllMasterNames();

    List<ScsSubMasterResponseDto> getSubMasterDetails(Integer  masterId);

    String addMaster(MasterRequestDto masterRequestDto);


    Page<DropdownResponseDto> getAllSubMaster(int page, int size, Integer scsMasterId, String scsMasterName, Integer scsSubMasterId, String scsSubMasterName,    String createdBy, LocalDate createdOn);

    Page<MasterResponseDto> getAllMasters(int page, int size, Integer scsMasterId, String scsMasterName, String createdBy,
                                          LocalDate createdOn);

    MasterResponseDto getMasterById(Integer id);

    String addSubMaster(SubMasterRequestDto dto);

    List<MasterResponseDto> getAllMasterDropdown();
}
