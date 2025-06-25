package com.im.admin.service.impl;

import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.entities.HierarchyMaster;
import com.im.admin.repositories.HierarchyMasterRepository;
import com.im.admin.service.HierarchyMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HierarchyMasterImpl implements HierarchyMasterService {

    @Autowired
    private HierarchyMasterRepository hierarchyMasterRepository;
    @Override
    public List<HierarchyMasterDto> getAllNameAndCode() {
        List<HierarchyMaster> all = hierarchyMasterRepository.findAll();
        return all.stream()
                .map(h -> new HierarchyMasterDto(h.getName(), h.getCode()))
                .collect(Collectors.toList());
    }
}
