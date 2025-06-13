package com.im.admin.service;

import com.im.admin.dto.ImSubMasterResponseDto;

import java.util.List;

public interface MasterService {

    List<String> getAllMasterNames();

    List<ImSubMasterResponseDto> getSubMasterDetails(String masterId);

}
