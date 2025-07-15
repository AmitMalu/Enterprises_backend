package com.im.admin.service.impl;

import com.im.admin.dto.StateDTO;
import com.im.admin.dto.StateRequestDTO;
import com.im.admin.dto.StateResponseDTO;
import com.im.admin.entities.Country;
import com.im.admin.entities.State;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.StateRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StateServiceImpl implements StateService {


    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CountryRepository countryRepository;


    @Override
    public CommonResponse<StateResponseDTO> save(StateRequestDTO dto) {
        State state = new State();
        state.setZone(dto.getZone());
        state.setStateName(dto.getStateName());
        state.setStateCode(dto.getStateCode());
        state.setRegionType(dto.getRegionType());
        state.setBlocked(dto.getBlocked());
        state.setCreatedBy(dto.getCreatedBy());

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
        state.setCountry(country);

        stateRepository.save(state);
        return CommonResponse.success(toDTO(state), "State saved successfully");
    }

    private StateResponseDTO toDTO(State state) {
        StateResponseDTO dto = new StateResponseDTO();
        dto.setId(state.getId());
        dto.setZone(state.getZone());
        dto.setStateName(state.getStateName());
        dto.setStateCode(state.getStateCode());
        dto.setRegionType(state.getRegionType());
        dto.setCountryName(state.getCountry().getCountryName());
        dto.setCountryId(state.getCountry().getId());
        dto.setBlocked(state.getBlocked());
        return dto;
    }

    @Override
    public CommonResponse<StateResponseDTO> update(Long id, StateRequestDTO dto) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));

        state.setZone(dto.getZone());
        state.setStateName(dto.getStateName());
        state.setStateCode(dto.getStateCode());
        state.setRegionType(dto.getRegionType());
        state.setBlocked(dto.getBlocked());
        state.setEditedBy(dto.getEditedBy());
        state.setEditedOn(LocalDateTime.now());

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
        state.setCountry(country);

        stateRepository.save(state);
        return CommonResponse.success(toDTO(state), "State updated successfully");
    }


    @Override
    public CommonResponse<StateResponseDTO> getById(Long id) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));
        return CommonResponse.success(toDTO(state), "State fetched successfully");
    }


    @Override
    public Page<StateResponseDTO> getAllStates(int page, int size, String zone, String stateName, String stateCode, String regionType, Long countryId, Boolean blocked) {
        List<State> allStates = stateRepository.findAll();

        List<State> filtered = allStates.stream()
                .filter(s -> zone == null || (s.getZone() != null && s.getZone().toLowerCase().contains(zone.toLowerCase())))
                .filter(s -> stateName == null || (s.getStateName() != null && s.getStateName().toLowerCase().contains(stateName.toLowerCase())))
                .filter(s -> stateCode == null || (s.getStateCode() != null && s.getStateCode().toLowerCase().contains(stateCode.toLowerCase())))
                .filter(s -> regionType == null || (s.getRegionType() != null && s.getRegionType().toLowerCase().contains(regionType.toLowerCase())))
                .filter(s -> countryId == null || (s.getCountry() != null && s.getCountry().getId().equals(countryId)))
                .filter(s -> blocked == null || (s.getBlocked() != null && s.getBlocked().equals(blocked)))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<State> pagedList = (start < end) ? filtered.subList(start, end) : List.of();

        List<StateResponseDTO> dtoList = pagedList.stream().map(this::toDTO).toList();

        return new PageImpl<>(dtoList, PageRequest.of(page, size), filtered.size());
    }


    @Override
    public List<StateDTO> getStatesByCountry(Long countryId) {
        List<State> stateList = stateRepository.findByCountryId(countryId);
        return stateList.stream()
                .map(state -> new StateDTO(state.getId(), state.getStateName()))
                .collect(Collectors.toList());
    }

}
