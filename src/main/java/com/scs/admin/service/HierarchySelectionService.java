package com.scs.admin.service;

import com.scs.admin.entities.SchemeHierarchySelection;
import com.scs.admin.request.SchemeHierarchySelectionRequest;
import com.scs.admin.response.SchemeHierarchySelectionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface HierarchySelectionService {

    Map<String, Object> saveHierarchySelection(SchemeHierarchySelectionRequest request);

    Map<String, Object> updateHierarchySelection(String id, SchemeHierarchySelectionRequest requestDTO);

    Page<SchemeHierarchySelectionResponseDTO> getAllSelections(
            String id,
            String schemeHierarchySelectionName,
            String schemeHierarchyCombination,
            String combinationHashCode,
            String createdBy,
            String createdOn,
            String editedBy,
            String editedOn,
            Pageable pageable
    );


    SchemeHierarchySelectionResponseDTO getSelectionById(String id);

}

