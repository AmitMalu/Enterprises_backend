package com.im.admin.service.impl;

import com.im.admin.dto.SchemeDTO;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.repositories.SchemeHierarchySelectionRepository;
import com.im.admin.repositories.SchemeRepository;
import com.im.admin.service.SchemeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class SchemeServiceImpl implements SchemeService {

    @Autowired
    private SchemeHierarchySelectionRepository schemeHierarchySelectionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    public List<Map<String, Object>> getAllSchemeHierarchySelections() {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findAll();

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getCombinationHash());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchSchemeHierarchy(String keyword) {
        List<SchemeHierarchySelection> selections = schemeHierarchySelectionRepository.findByCombinationHashContainingIgnoreCase(keyword);

        return selections.stream().map(selection -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", selection.getId());
            map.put("combinationHash", selection.getCombinationHash());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> saveScheme(SchemeDTO schemeDTO) {
        log.info("Saving new scheme: {}", schemeDTO);

        // Directly use request values to form combination hash
        String schemeCombinationHash = schemeDTO.getProductId() +"-"+
                schemeDTO.getSubProductId() +"-"+
                schemeDTO.getOperatorId() +"-"+
                schemeDTO.getVendorId() +"-"+
                schemeDTO.getSchemeHierarchySelectionId();

        // Check for duplicate Scheme Name
        if (schemeRepository.existsBySchemeName(schemeDTO.getSchemeName())) {
            throw new DuplicateEntryException("Scheme Name already exists!");
        }

        // Check for duplicate Scheme Code
        if (schemeRepository.existsBySchemeCode(schemeDTO.getSchemeCode())) {
            throw new DuplicateEntryException("Scheme Code already exists!");
        }

        // Check if a scheme already exists with the same prefix
//        if (schemeRepository.existsBySchemeCombinationHashCodeStartingWith(schemeCombinationHash)) {
//            throw new DuplicateEntryException("This scheme combination already exists!");
//        }


        Long maxId = schemeRepository.findMaxSchemeId();
        if (maxId == null) maxId = 0L;

        if (maxId >= 9999) {
            throw new IllegalStateException("Scheme ID limit exceeded (0001-9999). Cannot save new scheme.");
        }

        String formattedSchemeId = String.format("%04d", maxId + 1);
        String finalCombinationHashCode = schemeCombinationHash +"-"+ formattedSchemeId;

        Scheme scheme = new Scheme();
        scheme.setSchemeName(schemeDTO.getSchemeName());
        scheme.setSchemeCode(schemeDTO.getSchemeCode());
        scheme.setSchemeCombinationHashCode(finalCombinationHashCode);
        scheme.setCreatedBy("Admin");

        schemeRepository.save(scheme);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Scheme saved successfully.");
        response.put("schemeId", formattedSchemeId);
        response.put("schemeCombinationHashCode", finalCombinationHashCode);

        log.info("Scheme saved successfully: {}", response);
        return response;
    }
}
