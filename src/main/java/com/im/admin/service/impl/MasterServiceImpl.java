package com.im.admin.service.impl;

import com.im.admin.dto.ImSubMasterResponseDto;
import com.im.admin.entities.ImSubMaster;
import com.im.admin.repositories.ImSubMasterRepository;
import com.im.admin.repositories.MasterRepository;
import com.im.admin.service.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MasterServiceImpl implements MasterService {

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private ImSubMasterRepository imSubMasterRepository;

    @Override
    public List<String> getAllMasterNames() {
        return  masterRepository.findAllMasterNames();

    }

    @Override
    public List<ImSubMasterResponseDto> getSubMasterDetails(String masterId) {
        List<ImSubMaster> subMasters = imSubMasterRepository.findByImMasterId(masterId);

        // Map to response DTO
        return subMasters.stream().map(subMaster -> new ImSubMasterResponseDto(subMaster.getImSubMasterId(),
                subMaster.getImSubMasterName(), subMaster.getImMasterId())).collect(Collectors.toList());
    }
}
