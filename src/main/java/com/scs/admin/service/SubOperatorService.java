package com.scs.admin.service;

import com.scs.admin.entities.SubOperator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubOperatorService {

    SubOperator saveSubOperator(SubOperator subOperator);

    SubOperator updateSubOperator(String id, SubOperator subOperator);

    void deleteSubOperator(String id);

    Page<SubOperator> getAllSubOperators(Pageable pageable,
                                         List<String> id,
                                         String subOperatorName,
                                         String operatorCode,
                                         String operatorAlias,
                                         String category,
                                         String operatorName,
                                         Boolean isBlocked,
                                         String createdBy,
                                         String editedBy,
                                         LocalDate createdOn,
                                         LocalDate editedOn);

    Page<SubOperator> getAllSubOperatorsPaginated(Pageable pageable);

    Optional<SubOperator> getSubOperatorById(String id);
}
