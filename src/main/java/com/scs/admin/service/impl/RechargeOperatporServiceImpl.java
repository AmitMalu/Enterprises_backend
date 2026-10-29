package com.scs.admin.service.impl;

import com.scs.admin.entities.RechargeOperators;
import com.scs.admin.repositories.RechargeOperatorsRepository;
import com.scs.admin.service.RechargeOperatporService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RechargeOperatporServiceImpl implements RechargeOperatporService {

    @Autowired
    private RechargeOperatorsRepository rechargeOperatorsRepository;

    @Override
    public List<RechargeOperators> getAllActiveOperators() {
        return rechargeOperatorsRepository.findByActiveTrue();
    }

    @Override
    public List<RechargeOperators> getByType(String type) {
        return rechargeOperatorsRepository.findByTypeAndActiveTrue(type);
    }
}
