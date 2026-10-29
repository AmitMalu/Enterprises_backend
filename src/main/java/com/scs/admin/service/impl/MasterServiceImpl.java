package com.scs.admin.service.impl;


import com.scs.admin.dto.*;
import com.scs.admin.entities.ScsMaster;
import com.scs.admin.entities.ScsSubMaster;
import com.scs.admin.repositories.ScsSubMasterRepository;
import com.scs.admin.repositories.MasterRepository;
import com.scs.admin.service.MasterService;
import com.scs.admin.spec.DropdownSpecification;
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
    private ScsSubMasterRepository scsSubMasterRepository;

    @Override
    public List<String> getAllMasterNames() {
        return masterRepository.findAllMasterNames();

    }

    @Override
    public List<ScsSubMasterResponseDto> getSubMasterDetails(Integer masterId) {

        Optional<ScsMaster> masterOpt = masterRepository.findById(masterId);
        if (masterOpt.isEmpty()) {
            throw new RuntimeException("masterId not found");
        }
        ScsMaster scsMaster = masterOpt.get();

        List<ScsSubMaster> subMasters = scsSubMasterRepository.findByScsMasterId(masterId);

        // Map to response DTO
        return subMasters.stream().map(subMaster -> new ScsSubMasterResponseDto(subMaster.getScsSubMasterId(),
                subMaster.getScsSubMasterName(), subMaster.getScsMasterId(), scsMaster.getScsMasterName())).collect(Collectors.toList());
    }



    @Override
    public String addMaster(MasterRequestDto masterRequestDto) {
        Optional<ScsMaster> existing = masterRepository.findByScsMasterName(masterRequestDto.getScsMasterName());

        if (existing.isPresent()) {
            return "Master already exists.";
        }

        ScsMaster master = new ScsMaster();
        master.setScsMasterName(masterRequestDto.getScsMasterName());
        master.setCreatedBy(masterRequestDto.getCreatedBy());
        master.setCreatedOn(LocalDateTime.now());
        masterRepository.save(master);

        return "Master added successfully.";
    }

    @Override
    public Page<MasterResponseDto> getAllMasters(
            int page,
            int size,
            Integer scsMasterId,
            String scsMasterName,
            String createdBy,
            LocalDate createdOn
    ) {
        List<ScsMaster> allMasters = masterRepository.findAll();

        List<MasterResponseDto> filtered = allMasters.stream()
                .filter(m -> scsMasterId == null || m.getScsMasterId().equals(scsMasterId))
                .filter(m -> scsMasterName == null || (m.getScsMasterName() != null &&
                        m.getScsMasterName().toLowerCase().contains(scsMasterName.toLowerCase())))
                .filter(m -> createdBy == null || (m.getCreatedBy() != null &&
                        m.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(m -> createdOn == null ||
                        (m.getCreatedOn() != null &&
                                m.getCreatedOn().toLocalDate().equals(createdOn)))

                .map(m -> new MasterResponseDto(
                        m.getScsMasterId(),
                        m.getScsMasterName(),
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
      ScsMaster master  = masterRepository.findById(id)
              .orElseThrow(() -> new RuntimeException("Master not found with ID: " + id));
      return new MasterResponseDto(master.getScsMasterId(),master.getScsMasterName(),master.getCreatedBy(),master.getCreatedOn());
    }

    @Override
    public List<MasterResponseDto> getAllMasterDropdown() {
        return masterRepository.findAll().stream()
                .map(master -> new MasterResponseDto(master.getScsMasterId(), master.getScsMasterName(),master.getCreatedBy(),master.getCreatedOn()))
                .collect(Collectors.toList());
    }



    @Override
    public String addSubMaster(SubMasterRequestDto dto) {
        // Check if Master exists
        ScsMaster master = masterRepository.findById(dto.getScsMasterId())
                .orElseThrow(() -> new RuntimeException("Master not found with ID: " + dto.getScsMasterId()));

        // Check if SubMaster already exists under the given Master
        boolean exists = scsSubMasterRepository.existsByScsMasterIdAndScsSubMasterName(
                dto.getScsMasterId(), dto.getScsSubMasterName());

        if (exists) {
            return "Sub master already exists.";
        }

        // Create new SubMaster
        ScsSubMaster subMaster = new ScsSubMaster();
        subMaster.setScsMasterId(dto.getScsMasterId());
        subMaster.setScsSubMasterName(dto.getScsSubMasterName());
        subMaster.setCreatedBy(dto.getCreatedBy());
        subMaster.setCreatedOn(LocalDateTime.now());
//        subMaster.setScsSubMasterDesc(dto.getScsSubMasterName()); // Optional, reuse name

        scsSubMasterRepository.save(subMaster);
        return "Sub master added successfully.";
    }

    @Override
    public Page<DropdownResponseDto> getAllSubMaster(
            int page, int size,
            Integer scsMasterId, String scsMasterName,
            Integer scsSubMasterId, String scsSubMasterName,
            String createdBy, LocalDate createdOn
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rawPage = scsSubMasterRepository.getAllDropdowns(Pageable.unpaged()); // Fetch all

        List<DropdownResponseDto> filteredList = rawPage.getContent().stream()
                .map(row -> new DropdownResponseDto(
                        (Integer) row[0],
                        (String) row[1],
                        (Integer) row[2],
                        (String) row[3],
                        (String) row[4],
                        row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null
                ))
                .filter(dto -> scsMasterId == null || dto.getScsMasterId().equals(scsMasterId))
                .filter(dto -> scsMasterName == null || (dto.getScsMasterName() != null
                        && dto.getScsMasterName().toLowerCase().contains(scsMasterName.toLowerCase())))
                .filter(dto -> scsSubMasterId == null || dto.getScsSubMasterId().equals(scsSubMasterId))
                .filter(dto -> scsSubMasterName == null || (dto.getScsSubMasterName() != null
                        && dto.getScsSubMasterName().toLowerCase().contains(scsSubMasterName.toLowerCase())))
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

