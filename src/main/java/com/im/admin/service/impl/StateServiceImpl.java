package com.im.admin.service.impl;

import com.im.admin.entities.Country;
import com.im.admin.entities.State;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.StateRepository;
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
    public State createState(State state) {
        if (state.getCountry() == null || state.getCountry().getId() == null) {
            throw new RuntimeException("Country ID is required.");
        }

        // Fetch full Country from DB
        Country fullCountry = countryRepository.findById(state.getCountry().getId())
                .orElseThrow(() -> new RuntimeException("Country not found with ID: " + state.getCountry().getId()));

        state.setCountry(fullCountry);
        return stateRepository.save(state);
    }

    @Override
    public State updateState(Long id, State updatedState) {
        State existing = stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));

        // Fetch full country from DB if country ID is provided
        if (updatedState.getCountry() == null || updatedState.getCountry().getId() == null) {
            throw new RuntimeException("Country ID is required for update.");
        }

        Country fullCountry = countryRepository.findById(updatedState.getCountry().getId())
                .orElseThrow(() -> new RuntimeException("Country not found with ID: " + updatedState.getCountry().getId()));

        existing.setZone(updatedState.getZone());
        existing.setStateName(updatedState.getStateName());
        existing.setStateCode(updatedState.getStateCode());
        existing.setRegionType(updatedState.getRegionType());
        existing.setCountry(fullCountry); //  Set full country
        existing.setBlocked(updatedState.getBlocked());
        existing.setEditedOn(LocalDateTime.now());
        existing.setEditedBy(updatedState.getEditedBy());

        return stateRepository.save(existing);
    }


    @Override
    public State getStateById(Long id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));
    }

    @Override
    public Page<State> getFilteredStates(int page, int size,
                                         String zone, String stateName, String stateCode,
                                         String regionType, Boolean blocked,
                                         String createdBy, String editedBy) {

        List<State> all = stateRepository.findAll();

        List<State> filtered = all.stream()
                .filter(s -> zone == null || (s.getZone() != null && s.getZone().toLowerCase().contains(zone.toLowerCase())))
                .filter(s -> stateName == null || (s.getStateName() != null && s.getStateName().toLowerCase().contains(stateName.toLowerCase())))
                .filter(s -> stateCode == null || (s.getStateCode() != null && s.getStateCode().toLowerCase().contains(stateCode.toLowerCase())))
                .filter(s -> regionType == null || (s.getRegionType() != null && s.getRegionType().toLowerCase().contains(regionType.toLowerCase())))
                .filter(s -> blocked == null || (s.getBlocked() != null && s.getBlocked().equals(blocked)))
                .filter(s -> createdBy == null || (s.getCreatedBy() != null && s.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(s -> editedBy == null || (s.getEditedBy() != null && s.getEditedBy().toLowerCase().contains(editedBy.toLowerCase())))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        List<State> pagedList = (start < end) ? filtered.subList(start, end) : List.of();
        return new PageImpl<>(pagedList, PageRequest.of(page, size), filtered.size());
    }
}
