package com.im.admin.service.impl;

import com.im.admin.entities.HierarchyMaster;
import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.entities.SchemeHierarchySelectionMapping;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.repositories.HierarchyMasterRepository;
import com.im.admin.repositories.HierarchySelectionRepository;
import com.im.admin.service.HierarchySelectionService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    @Transactional
    @Override
    public Map<String, Object> saveHierarchySelection(List<String> hierarchyNames) {
        log.info("Saving hierarchy selection: {}", hierarchyNames);

        Set<HierarchyMaster> hierarchies = hierarchyMasterRepository.findByNameIn(hierarchyNames);

        String combinationHash = generateHash(hierarchyNames);
        if (hierarchySelectionRepository.existsByCombinationHash(combinationHash)) {
            throw new DuplicateEntryException("This hierarchy selection already exists!");
        }

        String nextId = commonMethods.generateNextSequentialId(hierarchySelectionRepository.findMaxId());
        if (nextId == null) {
            throw new IllegalStateException("ID limit exceeded! Cannot store more than 9999 records.");
        }

        SchemeHierarchySelection selection = new SchemeHierarchySelection();
        selection.setId(nextId);
        selection.setCombinationHash(combinationHash);
        selection.setCreatedBy("system");
        selection.setEditedBy("Admin");

        Set<SchemeHierarchySelectionMapping> mappings = new HashSet<>();
        for (HierarchyMaster hierarchy : hierarchies) {
            SchemeHierarchySelectionMapping mapping = new SchemeHierarchySelectionMapping();
            mapping.setSelection(selection);
            mapping.setHierarchy(hierarchy);
            mapping.setHierarchyCode(hierarchy.getCode());
            mappings.add(mapping);
        }

        selection.setSelectedHierarchies(mappings);
        hierarchySelectionRepository.save(selection);

        // Prepare response data
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hierarchy selection saved successfully.");
        response.put("selectionId", selection.getId());
        response.put("combinationHash", selection.getCombinationHash());
        response.put("selectedHierarchies", mappings.stream()
                .map(m -> Map.of("name", m.getHierarchy().getName(), "code", m.getHierarchy().getCode()))
                .collect(Collectors.toList()));

        log.info("Hierarchy selection saved successfully.");
        return response;
    }

    @Transactional
    @Override
    public void updateHierarchySelection(String id, List<String> hierarchyNames) {
        log.info("Updating hierarchy selection with ID: {}", id);

        SchemeHierarchySelection existingSelection = hierarchySelectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchy selection not found!"));

        Set<HierarchyMaster> hierarchies = hierarchyMasterRepository.findByNameIn(hierarchyNames);
        if (hierarchies.size() != hierarchyNames.size()) {
            throw new IllegalArgumentException("Invalid hierarchy names provided.");
        }

        String newCombinationHash = generateHash(hierarchyNames);
        if (!existingSelection.getCombinationHash().equals(newCombinationHash) &&
                hierarchySelectionRepository.existsByCombinationHash(newCombinationHash)) {
            throw new DuplicateEntryException("This hierarchy selection already exists!");
        }

        // Clear existing mappings before adding new ones
        existingSelection.getSelectedHierarchies().clear();

        Set<SchemeHierarchySelectionMapping> newMappings = new HashSet<>();
        for (HierarchyMaster hierarchy : hierarchies) {
            SchemeHierarchySelectionMapping mapping = new SchemeHierarchySelectionMapping();
            mapping.setSelection(existingSelection);
            mapping.setHierarchy(hierarchy);
            mapping.setHierarchyCode(hierarchy.getCode()); // Store hierarchy code

            newMappings.add(mapping);
        }

        existingSelection.setCombinationHash(newCombinationHash);
        existingSelection.setSelectedHierarchies(newMappings);

        hierarchySelectionRepository.save(existingSelection);
        log.info("Hierarchy selection updated successfully.");
    }

    @Override
    public List<SchemeHierarchySelection> getAllSelections(Pageable pageable) {
        return hierarchySelectionRepository.findAll(pageable).getContent();
    }

    @Override
    public SchemeHierarchySelection getSelectionById(String id) {
        log.info("Fetching hierarchy selection by ID: {}", id);
        return hierarchySelectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchy selection not found!"));
    }

    private String generateHash(List<String> hierarchyNames) {
        Collections.sort(hierarchyNames);
        return String.join("-", hierarchyNames);
    }
}

