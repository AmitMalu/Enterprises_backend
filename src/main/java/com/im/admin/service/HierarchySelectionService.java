package com.im.admin.service;

import com.im.admin.entities.SchemeHierarchySelection;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface HierarchySelectionService {

    Map<String, Object> saveHierarchySelection(List<String> hierarchyNames);

    void updateHierarchySelection(String id, List<String> hierarchyNames);

    List<SchemeHierarchySelection> getAllSelections(Pageable pageable);

    SchemeHierarchySelection getSelectionById(String id);

}

