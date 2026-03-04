package com.im.admin.service.impl;

import com.im.admin.entities.RechargeOperators;
import com.im.admin.repositories.RechargeOperatorsRepository;
import com.im.admin.service.RechargeOperatporService;
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
