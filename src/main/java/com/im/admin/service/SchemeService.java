package com.im.admin.service;


import com.im.admin.dto.SchemeDTO;

import java.util.List;
import java.util.Map;

public interface SchemeService {

    List<Map<String, Object>> getAllSchemeHierarchySelections();

    List<Map<String, Object>> searchSchemeHierarchy(String keyword);

    Map<String, Object> saveScheme(SchemeDTO schemeDTO);


}
