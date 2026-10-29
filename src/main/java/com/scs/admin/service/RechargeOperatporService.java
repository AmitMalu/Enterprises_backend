package com.scs.admin.service;

import com.scs.admin.entities.RechargeOperators;
import org.springframework.expression.spel.ast.Operator;

import java.util.List;

public interface RechargeOperatporService {
    List<RechargeOperators> getAllActiveOperators();

    List<RechargeOperators> getByType(String type);
}
