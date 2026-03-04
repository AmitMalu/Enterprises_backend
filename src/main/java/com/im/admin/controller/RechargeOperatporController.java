package com.im.admin.controller;

import com.im.admin.entities.RechargeOperators;
import com.im.admin.service.RechargeOperatporService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recharge/operators")
public class RechargeOperatporController {

    @Autowired
    private RechargeOperatporService rechargeOperatporService;

    @GetMapping
    public ResponseEntity<List<RechargeOperators>> getAllOperators() {
        return ResponseEntity.ok(
                rechargeOperatporService.getAllActiveOperators());
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<RechargeOperators>> getByType(
            @PathVariable String type) {

        return ResponseEntity.ok(
                rechargeOperatporService.getByType(type));
    }
}
