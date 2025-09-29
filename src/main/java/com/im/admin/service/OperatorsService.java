package com.im.admin.service;

import com.im.admin.dto.OperatorDropdownResponseDTO;
import com.im.admin.entities.Operators;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OperatorsService {

    Operators saveOperator(Operators operator);

    Operators updateOperator(String id, Operators operator);

    void deleteOperator(String id);

    Page<Operators> getAllOperators(Pageable pageable, List<String> id, String operatorCategory, String operatorCode,
                                    String operatorName, String operatorAlias, String alias, String productCode,
                                    String productId, String subProductCode, String subProductId, Boolean isActive,
                                    String createdBy, String editedBy,
                                    LocalDate createdOn,
                                    LocalDate editedOn);



    Operators getOperatorById(String id);

    List<OperatorDropdownResponseDTO> getActiveOperatorsBySubProductId(String subProductId);

    List<OperatorDropdownResponseDTO> getActiveOperators();

}

