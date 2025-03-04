package com.im.admin.service;


import com.im.admin.dto.OperatorSourceDropdownDto;
import com.im.admin.entities.OperatorSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OperatorSourceService {

    OperatorSource saveOperatorSource(OperatorSource operatorSource);

    OperatorSource updateOperatorSource(Long id, OperatorSource operatorSource);

    List<OperatorSource> getAllOperatorSources();

    Page<OperatorSource> getAllOperatorSourcesWithPagination(Pageable pageable);

    Optional<OperatorSource> getOperatorSourceById(Long id);

    void deleteOperatorSource(Long id);

    List<Map<String, Object>> getOperatorDropdown();

}

