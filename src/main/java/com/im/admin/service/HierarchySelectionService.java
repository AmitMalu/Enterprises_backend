package com.im.admin.service;

import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.request.SchemeHierarchySelectionRequest;
import com.im.admin.response.SchemeHierarchySelectionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface HierarchySelectionService {

    Map<String, Object> saveHierarchySelection(SchemeHierarchySelectionRequest request);

    Map<String, Object> updateHierarchySelection(String id, SchemeHierarchySelectionRequest requestDTO);

    Page<SchemeHierarchySelectionResponseDTO> getAllSelections(Pageable pageable);

    SchemeHierarchySelectionResponseDTO getSelectionById(String id);

}

