package com.im.admin.service.impl;

import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.entities.District;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.DistrictRepository;
import com.im.admin.repositories.StateRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistrictService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistrictServiceImpl implements DistrictService {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private StateRepository stateRepository;

    @Override
    public CommonResponse<DistrictResponseDTO> save(DistrictRequestDTO dto) {
        District district = new District();
        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setCreatedBy(dto.getCreatedBy());

        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));

        districtRepository.save(district);

        return CommonResponse.success(toDTO(district), "District saved successfully");
    }

    @Override
    public CommonResponse<DistrictResponseDTO> update(Long id, DistrictRequestDTO dto) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("District not found"));

        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setEditedOn(LocalDateTime.now());
        district.setEditedBy(dto.getEditedBy());

        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));

        districtRepository.save(district);
        return CommonResponse.success(toDTO(district), "District updated successfully");
    }

    @Override
    public CommonResponse<DistrictResponseDTO> getById(Long id) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("District not found"));
        return CommonResponse.success(toDTO(district), "District fetched");
    }

    @Override
    public CommonResponse<Page<DistrictResponseDTO>> getAll(int page, int size,
                                                            String districtName, String districtCode, Boolean blocked, String createdBy, String editedBy) {

        Pageable pageable = PageRequest.of(page, size);

        Specification<District> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (districtName != null && !districtName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("districtName")), "%" + districtName.toLowerCase() + "%"));
            }
            if (districtCode != null && !districtCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("districtCode")), "%" + districtCode.toLowerCase() + "%"));
            }
            if (blocked != null) {
                predicates.add(cb.equal(root.get("blocked"), blocked));
            }
            if (createdBy != null && !createdBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }
            if (editedBy != null && !editedBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<District> districtPage = districtRepository.findAll(spec, pageable);
        Page<DistrictResponseDTO> dtoPage = districtPage.map(this::toDTO);
        return CommonResponse.success(dtoPage, "Districts fetched successfully");
    }


    private DistrictResponseDTO toDTO(District district) {
        DistrictResponseDTO dto = new DistrictResponseDTO();
        dto.setId(district.getId());
        dto.setDistrictName(district.getDistrictName());
        dto.setDistrictCode(district.getDistrictCode());
        dto.setBlocked(district.getBlocked());
        dto.setCountryName(district.getCountry().getCountryName());
        dto.setStateName(district.getState().getStateName());
        dto.setStateId(district.getState().getId());
        dto.setCountryId(district.getCountry().getId());
        return dto;
    }

    @Override
    public List<DistrictDTO> getDistrictsByState(Long stateId) {
        List<District> districts = districtRepository.findByStateId(stateId);
        return districts.stream()
                .map(d -> new DistrictDTO(d.getId(), d.getDistrictName()))
                .collect(Collectors.toList());
    }

}

