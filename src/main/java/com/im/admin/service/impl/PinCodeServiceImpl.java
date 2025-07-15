package com.im.admin.service.impl;

import com.im.admin.dto.PinCodeDTO;
import com.im.admin.dto.PinCodeRequestDTO;
import com.im.admin.dto.PinCodeResponseDTO;
import com.im.admin.entities.PinCode;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PinCodeLocationResponse;
import com.im.admin.service.PinCodeService;
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
public class PinCodeServiceImpl implements PinCodeService {

    @Autowired
    private PinCodeRepository pinCodeRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CityRepository cityRepository;

    @Override
    public CommonResponse<PinCodeResponseDTO> save(PinCodeRequestDTO dto) {
        PinCode pin = new PinCode();
        pin.setPinCode(dto.getPinCode());
        pin.setBlocked(dto.getBlocked());
        pin.setCreatedBy(dto.getCreatedBy());

        pin.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        pin.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));
        pin.setDistrict(districtRepository.findById(dto.getDistrictId()).orElseThrow(() -> new RuntimeException("District not found")));
        pin.setCity(cityRepository.findById(dto.getCityId()).orElseThrow(() -> new RuntimeException("City not found")));

        pinCodeRepository.save(pin);
        return CommonResponse.success(toDTO(pin), "Pin code saved successfully");
    }

    @Override
    public CommonResponse<PinCodeResponseDTO> update(Long id, PinCodeRequestDTO dto) {
        PinCode pin = pinCodeRepository.findById(id).orElseThrow(() -> new RuntimeException("Pin code not found"));

        pin.setPinCode(dto.getPinCode());
        pin.setBlocked(dto.getBlocked());
        pin.setEditedBy(dto.getCreatedBy());
        pin.setEditedOn(LocalDateTime.now());

        pin.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        pin.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));
        pin.setDistrict(districtRepository.findById(dto.getDistrictId()).orElseThrow(() -> new RuntimeException("District not found")));
        pin.setCity(cityRepository.findById(dto.getCityId()).orElseThrow(() -> new RuntimeException("City not found")));

        pinCodeRepository.save(pin);
        return CommonResponse.success(toDTO(pin), "Pin code updated successfully");
    }

    @Override
    public CommonResponse<PinCodeResponseDTO> getById(Long id) {
        PinCode pin = pinCodeRepository.findById(id).orElseThrow(() -> new RuntimeException("Pin code not found"));
        return CommonResponse.success(toDTO(pin), "Pin code fetched successfully");
    }

    @Override
    public CommonResponse<Page<PinCodeResponseDTO>> getAll(int page, int size,
                                                           String pinCode, Boolean blocked, String createdBy, String editedBy) {

        Pageable pageable = PageRequest.of(page, size);

        Specification<PinCode> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (pinCode != null && !pinCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pinCode")), "%" + pinCode.toLowerCase() + "%"));
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

        Page<PinCode> pageResult = pinCodeRepository.findAll(spec, pageable);
        Page<PinCodeResponseDTO> dtoPage = pageResult.map(this::toDTO);
        return CommonResponse.success(dtoPage, "Pin codes fetched successfully");
    }

    @Override
    public List<PinCodeDTO> getPinCodesByCity(Long cityId) {
        List<PinCode> pins = pinCodeRepository.findByCity_Id(cityId);
        return pins.stream()
                .map(p -> new PinCodeDTO(p.getId(), p.getPinCode()))
                .collect(Collectors.toList());
    }

    private PinCodeResponseDTO toDTO(PinCode pin) {
        PinCodeResponseDTO dto = new PinCodeResponseDTO();
        dto.setId(pin.getId());
        dto.setPinCode(pin.getPinCode());
        dto.setBlocked(pin.getBlocked());
        dto.setCountryName(pin.getCountry().getCountryName());
        dto.setStateName(pin.getState().getStateName());
        dto.setDistrictName(pin.getDistrict().getDistrictName());
        dto.setCityName(pin.getCity().getCityName());
        return dto;
    }

    @Override
    public PinCodeLocationResponse lookupByPinCode(String pinCode) {
        PinCode pin = pinCodeRepository.findByPinCode(pinCode)
                .orElseThrow(() -> new RuntimeException("Pin code not found"));

        PinCodeLocationResponse response = new PinCodeLocationResponse();
        response.setPinCode(pin.getPinCode());
        response.setCountryName(pin.getCountry().getCountryName());
        response.setStateName(pin.getState().getStateName());
        response.setDistrictName(pin.getDistrict().getDistrictName());
        response.setCityName(pin.getCity().getCityName());

        return response;
    }

}

