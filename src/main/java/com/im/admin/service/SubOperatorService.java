package com.im.admin.service;

import com.im.admin.entities.SubOperator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SubOperatorService {

    SubOperator saveSubOperator(SubOperator subOperator, String createdBy);

    SubOperator updateSubOperator(String id, SubOperator subOperator, String editedBy);

    void deleteSubOperator(String id);

    List<SubOperator> getAllSubOperators();

    Page<SubOperator> getAllSubOperatorsPaginated(Pageable pageable);

    Optional<SubOperator> getSubOperatorById(String id);
}
