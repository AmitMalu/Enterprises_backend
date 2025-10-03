package com.im.admin.service.impl;


import com.im.admin.dto.*;
import com.im.admin.entities.ImMaster;
import com.im.admin.entities.ImSubMaster;
import com.im.admin.repositories.ImSubMasterRepository;
import com.im.admin.repositories.MasterRepository;
import com.im.admin.service.MasterService;
import com.im.admin.spec.DropdownSpecification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    public String addMaster(MasterRequestDto masterRequestDto) {
        Optional<ImMaster> existing = masterRepository.findByImMasterName(masterRequestDto.getImMasterName());

        if (existing.isPresent()) {
            return "Master already exists.";
        }

        ImMaster master = new ImMaster();
        master.setImMasterName(masterRequestDto.getImMasterName());
        master.setCreatedBy(masterRequestDto.getCreatedBy());
        master.setCreatedOn(LocalDateTime.now());
        masterRepository.save(master);

        return "Master added successfully.";
    }

    @Override
    public Page<MasterResponseDto> getAllMasters(
            int page,
            int size,
            Integer imMasterId,
            String imMasterName,
            String createdBy,
            LocalDate createdOn
    ) {
        List<ImMaster> allMasters = masterRepository.findAll();

        List<MasterResponseDto> filtered = allMasters.stream()
                .filter(m -> imMasterId == null || m.getImMasterId().equals(imMasterId))
                .filter(m -> imMasterName == null || (m.getImMasterName() != null &&
                        m.getImMasterName().toLowerCase().contains(imMasterName.toLowerCase())))
                .filter(m -> createdBy == null || (m.getCreatedBy() != null &&
                        m.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(m -> createdOn == null ||
                        (m.getCreatedOn() != null &&
                                m.getCreatedOn().toLocalDate().equals(createdOn)))

                .map(m -> new MasterResponseDto(
                        m.getImMasterId(),
                        m.getImMasterName(),
                        m.getCreatedBy(),
                        m.getCreatedOn()
                ))
                .sorted(Comparator.comparing(MasterResponseDto::getCreatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }


    @Override
    public MasterResponseDto getMasterById(Integer id) {
      ImMaster master  = masterRepository.findById(id)
              .orElseThrow(() -> new RuntimeException("Master not found with ID: " + id));
      return new MasterResponseDto(master.getImMasterId(),master.getImMasterName(),master.getCreatedBy(),master.getCreatedOn());
    }

    @Override
    public List<MasterResponseDto> getAllMasterDropdown() {
        return masterRepository.findAll().stream()
                .map(master -> new MasterResponseDto(master.getImMasterId(), master.getImMasterName(),master.getCreatedBy(),master.getCreatedOn()))
                .collect(Collectors.toList());
    }



    @Override
    public String addSubMaster(SubMasterRequestDto dto) {
        // Check if Master exists
        ImMaster master = masterRepository.findById(dto.getImMasterId())
                .orElseThrow(() -> new RuntimeException("Master not found with ID: " + dto.getImMasterId()));

        // Check if SubMaster already exists under the given Master
        boolean exists = imSubMasterRepository.existsByImMasterIdAndImSubMasterName(
                dto.getImMasterId(), dto.getImSubMasterName());

        if (exists) {
            return "Sub master already exists.";
        }

        // Create new SubMaster
        ImSubMaster subMaster = new ImSubMaster();
        subMaster.setImMasterId(dto.getImMasterId());
        subMaster.setImSubMasterName(dto.getImSubMasterName());
        subMaster.setCreatedBy(dto.getCreatedBy());
        subMaster.setCreatedOn(LocalDateTime.now());
//        subMaster.setImSubMasterDesc(dto.getImSubMasterName()); // Optional, reuse name

        imSubMasterRepository.save(subMaster);
        return "Sub master added successfully.";
    }

    @Override
    public Page<DropdownResponseDto> getAllSubMaster(
            int page, int size,
            Integer imMasterId, String imMasterName,
            Integer imSubMasterId, String imSubMasterName,
            String createdBy, LocalDate createdOn
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rawPage = imSubMasterRepository.getAllDropdowns(Pageable.unpaged()); // Fetch all

        List<DropdownResponseDto> filteredList = rawPage.getContent().stream()
                .map(row -> new DropdownResponseDto(
                        (Integer) row[0],
                        (String) row[1],
                        (Integer) row[2],
                        (String) row[3],
                        (String) row[4],
                        row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null
                ))
                .filter(dto -> imMasterId == null || dto.getImMasterId().equals(imMasterId))
                .filter(dto -> imMasterName == null || (dto.getImMasterName() != null
                        && dto.getImMasterName().toLowerCase().contains(imMasterName.toLowerCase())))
                .filter(dto -> imSubMasterId == null || dto.getImSubMasterId().equals(imSubMasterId))
                .filter(dto -> imSubMasterName == null || (dto.getImSubMasterName() != null
                        && dto.getImSubMasterName().toLowerCase().contains(imSubMasterName.toLowerCase())))
                .filter(dto -> createdBy == null || (dto.getCreatedBy() != null
                        && dto.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(dto -> createdOn == null || (dto.getCreatedOn() != null
                        && dto.getCreatedOn().toLocalDate().equals(createdOn)))
                // Sort by createdOn descending (latest first)
                .sorted((a, b) -> {
                    if (a.getCreatedOn() == null && b.getCreatedOn() == null) return 0;
                    if (a.getCreatedOn() == null) return 1;
                    if (b.getCreatedOn() == null) return -1;
                    return b.getCreatedOn().compareTo(a.getCreatedOn());
                })
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredList.size());
        Page<DropdownResponseDto> pageResult =
                new PageImpl<>(filteredList.subList(start, end), pageable, filteredList.size());

        return pageResult;
    }

}

