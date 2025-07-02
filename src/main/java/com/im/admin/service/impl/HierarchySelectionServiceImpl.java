package com.im.admin.service.impl;

import com.im.admin.entities.HierarchyMaster;
import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.entities.SchemeHierarchySelectionMapping;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.repositories.HierarchyMasterRepository;
import com.im.admin.repositories.HierarchySelectionRepository;
import com.im.admin.request.SchemeHierarchySelectionRequest;
import com.im.admin.response.SchemeHierarchySelectionResponseDTO;
import com.im.admin.service.HierarchySelectionService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HierarchySelectionServiceImpl implements HierarchySelectionService {

    @Autowired
    private HierarchyMasterRepository hierarchyMasterRepository;

    @Autowired
    private HierarchySelectionRepository hierarchySelectionRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    @Transactional
    public Map<String, Object> saveHierarchySelection(SchemeHierarchySelectionRequest requestDTO) {
        log.info("Saving hierarchy selection: {}", requestDTO.getSchemeHierarchyCombination());

        // Step 1: Get hierarchies by name
        Set<HierarchyMaster> hierarchiesSet = hierarchyMasterRepository.findByNameIn(requestDTO.getSchemeHierarchyCombination());

        // Step 2: Sort the result in the same order as input
        List<HierarchyMaster> sortedHierarchies = requestDTO.getSchemeHierarchyCombination().stream().map(name -> hierarchiesSet.stream().filter(h -> h.getName().equalsIgnoreCase(name)).findFirst().orElseThrow(() -> new RuntimeException("Hierarchy not found: " + name))).toList();

        // Step 3: Generate name-based combination: e.g., "Distributor-Partner-Retailer-SuperDistributor"
        String nameCombination = sortedHierarchies.stream().map(HierarchyMaster::getName).collect(Collectors.joining("-"));

        // Step 5: Check for duplicate by nameCombination
        if (hierarchySelectionRepository.existsBySchemeHierarchyCombination(nameCombination)) {
            throw new DuplicateEntryException("This hierarchy combination already exists!");
        }

        // Step 4: Generate code-based combination: e.g., "DT-PT-RT-SD"
        String codeCombination = sortedHierarchies.stream().map(HierarchyMaster::getCode).collect(Collectors.joining("-"));

        // Step 6: Check for duplicate by selection name
        if (hierarchySelectionRepository.existsBySchemeSelectionHierarchyName(requestDTO.getSchemeHierarchySelectionName())) {
            throw new DuplicateEntryException("This hierarchy selection name already exists!");
        }

        // Step 6: Generate ID
        String nextId = commonMethods.generateNextSequentialId(hierarchySelectionRepository.findMaxId());
        if (nextId == null) {
            throw new IllegalStateException("ID limit exceeded! Cannot store more than 9999 records.");
        }

        // Step 7: Save entity
        SchemeHierarchySelection selection = new SchemeHierarchySelection();
        selection.setId(nextId);
        selection.setSchemeSelectionHierarchyName(requestDTO.getSchemeHierarchySelectionName());
        selection.setSchemeHierarchyCombination(nameCombination);
        selection.setCombinationHashCode(codeCombination);
        selection.setCreatedBy(requestDTO.getCreatedBy());
        selection.setCreatedOn(LocalDateTime.now());

        Set<SchemeHierarchySelectionMapping> mappings = new HashSet<>();
        for (HierarchyMaster hierarchy : sortedHierarchies) {
            SchemeHierarchySelectionMapping mapping = new SchemeHierarchySelectionMapping();
            mapping.setSelection(selection);
            mapping.setHierarchy(hierarchy);
            mapping.setHierarchyCode(hierarchy.getCode());
            mappings.add(mapping);
        }

        selection.setSelectedHierarchies(mappings);
        hierarchySelectionRepository.save(selection);

        // Step 8: Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("selectionId", selection.getId());
        response.put("schemeHierarchyCombination", nameCombination);
        response.put("combinationHashCode", codeCombination);
        response.put("hierarchySelectionName", selection.getSchemeSelectionHierarchyName());
        response.put("selectedHierarchies", mappings.stream().map(m -> Map.of("name", m.getHierarchy().getName(), "code", m.getHierarchy().getCode())).collect(Collectors.toList()));

        log.info("Hierarchy selection saved successfully.");
        return response;
    }


    @Override
    @Transactional
    public Map<String, Object> updateHierarchySelection(String id, SchemeHierarchySelectionRequest requestDTO) {
        log.info("Updating hierarchy selection with ID: {}", id);

        SchemeHierarchySelection existingSelection = hierarchySelectionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hierarchy selection not found!"));

        Set<HierarchyMaster> hierarchiesSet = hierarchyMasterRepository.findByNameIn(requestDTO.getSchemeHierarchyCombination());

        List<HierarchyMaster> sortedHierarchies = requestDTO.getSchemeHierarchyCombination().stream().map(name -> hierarchiesSet.stream().filter(h -> h.getName().equalsIgnoreCase(name)).findFirst().orElseThrow(() -> new RuntimeException("Hierarchy not found: " + name))).toList();

        String nameCombination = sortedHierarchies.stream().map(HierarchyMaster::getName).collect(Collectors.joining("-"));

        String codeCombination = sortedHierarchies.stream().map(HierarchyMaster::getCode).collect(Collectors.joining("-"));

        if (!existingSelection.getSchemeHierarchyCombination().equals(nameCombination) && hierarchySelectionRepository.existsBySchemeHierarchyCombination(nameCombination)) {
            throw new DuplicateEntryException("This hierarchy combination already exists!");
        }

        if (!existingSelection.getSchemeSelectionHierarchyName().equals(requestDTO.getSchemeHierarchySelectionName()) && hierarchySelectionRepository.existsBySchemeSelectionHierarchyName(requestDTO.getSchemeHierarchySelectionName())) {
            throw new DuplicateEntryException("This hierarchy selection name already exists!");
        }

        existingSelection.setSchemeHierarchyCombination(nameCombination);
        existingSelection.setCombinationHashCode(codeCombination);
        existingSelection.setSchemeSelectionHierarchyName(requestDTO.getSchemeHierarchySelectionName());
        existingSelection.setEditedBy(requestDTO.getEditedBy());
        existingSelection.setEditedOn(LocalDateTime.now());

        // SAFELY clear and update existing mappings
        Set<SchemeHierarchySelectionMapping> currentMappings = existingSelection.getSelectedHierarchies();
        currentMappings.clear(); // Remove orphans

        for (HierarchyMaster hierarchy : sortedHierarchies) {
            SchemeHierarchySelectionMapping mapping = new SchemeHierarchySelectionMapping();
            mapping.setSelection(existingSelection);
            mapping.setHierarchy(hierarchy);
            mapping.setHierarchyCode(hierarchy.getCode());
            currentMappings.add(mapping); // Add to existing set
        }

        // No need to call setSelectedHierarchies(newMappings)
        hierarchySelectionRepository.save(existingSelection);


        Map<String, Object> response = new HashMap<>();
        response.put("selectionId", existingSelection.getId());
        response.put("schemeHierarchyCombination", nameCombination);
        response.put("combinationHashCode", codeCombination);
        response.put("hierarchySelectionName", existingSelection.getSchemeSelectionHierarchyName());

        log.info("Hierarchy selection updated successfully.");
        return response;
    }

    @Override
    public Page<SchemeHierarchySelectionResponseDTO> getAllSelections(Pageable pageable) {
        return hierarchySelectionRepository.findAll(pageable).map(SchemeHierarchySelectionResponseDTO::new);
    }

    @Override
    public SchemeHierarchySelectionResponseDTO getSelectionById(String id) {
        log.info("Fetching hierarchy selection by ID: {}", id);
        SchemeHierarchySelection selection = hierarchySelectionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hierarchy selection not found!"));

        return new SchemeHierarchySelectionResponseDTO(selection);
    }

}

