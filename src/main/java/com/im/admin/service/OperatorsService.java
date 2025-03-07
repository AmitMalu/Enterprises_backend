package com.im.admin.service;

import com.im.admin.entities.Operators;
import org.springframework.data.domain.Page;

public interface OperatorsService {

    Operators saveOperator(Operators operator);

    Operators updateOperator(String id, Operators operator);

    void deleteOperator(String id);

    Page<Operators> getAllOperators(int page, int size);

    Operators getOperatorById(String id);
}

