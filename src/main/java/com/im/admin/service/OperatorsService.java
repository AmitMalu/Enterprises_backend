package com.im.admin.service;

import com.im.admin.dto.OperatorDropdownResponseDTO;
import com.im.admin.entities.Operators;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface OperatorsService {

    Operators saveOperator(Operators operator);

    Operators updateOperator(String id, Operators operator);

    void deleteOperator(String id);

    Page<Operators> getAllOperators(int page, int size);

    Operators getOperatorById(String id);

    List<OperatorDropdownResponseDTO> getActiveOperatorsBySubProductId(String subProductId);

    List<OperatorDropdownResponseDTO> getActiveOperators();

}

