package com.im.admin.service.impl;

import com.im.admin.dto.DropdownRequestDto;
import com.im.admin.dto.DropdownResponseDto;
import com.im.admin.dto.ImSubMasterResponseDto;
import com.im.admin.entities.ImMaster;
import com.im.admin.entities.ImSubMaster;
import com.im.admin.repositories.ImSubMasterRepository;
import com.im.admin.repositories.MasterRepository;
import com.im.admin.service.MasterService;
import com.im.admin.spec.DropdownSpecification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MasterServiceImpl implements MasterService {

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private ImSubMasterRepository imSubMasterRepository;

    @Override
    public List<String> getAllMasterNames() {
        return masterRepository.findAllMasterNames();

    }

    @Override
    public List<ImSubMasterResponseDto> getSubMasterDetails(Integer masterId) {

        Optional<ImMaster> masterOpt = masterRepository.findById(masterId);
        if (masterOpt.isEmpty()) {
            throw new RuntimeException("masterId not found");
        }
        ImMaster imMaster = masterOpt.get();

        List<ImSubMaster> subMasters = imSubMasterRepository.findByImMasterId(masterId);

        // Map to response DTO
        return subMasters.stream().map(subMaster -> new ImSubMasterResponseDto(subMaster.getImSubMasterId(),
                subMaster.getImSubMasterName(), subMaster.getImMasterId(), imMaster.getImMasterName())).collect(Collectors.toList());
    }


    @Override
    public String addDropdown(DropdownRequestDto dto) {
        Optional<ImMaster> masterOpt = masterRepository.findByImMasterName(dto.getImMasterName());

        ImMaster master;
        if (masterOpt.isEmpty()) {
            // Add new im_master
            master = new ImMaster();
            master.setImMasterName(dto.getImMasterName());
            master = masterRepository.save(master);
        } else {
            master = masterOpt.get();
        }


        // Check if sub master already exists
        boolean exists = imSubMasterRepository.existsByImMasterIdAndImSubMasterName(
                master.getImMasterId(), dto.getImSubMasterName());

        if (!exists) {
            ImSubMaster sub = new ImSubMaster();
            sub.setImMasterId(master.getImMasterId());
            sub.setImSubMasterName(dto.getImSubMasterName());
//            sub.setImSubMasterDesc(dto.getImSubMasterName());
            imSubMasterRepository.save(sub);
            return "Sub master added successfully.";
        } else {
            return "Sub master already exists.";
        }
    }


    @Override
    public Page<DropdownResponseDto> getAllDropdowns(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rawPage = imSubMasterRepository.getAllDropdowns(pageable);

        return rawPage.map(row -> new DropdownResponseDto(
                (Integer) row[0],
                (String) row[1],
                (Integer) row[2],
                (String) row[3]
        ));
    }

}
