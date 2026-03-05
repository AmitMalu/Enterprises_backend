package com.im.admin.controller;

import com.im.admin.entities.RechargeOperators;
import com.im.admin.response.CommonResponse;
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
    public ResponseEntity<CommonResponse<List<RechargeOperators>>> getAllOperators() {

        List<RechargeOperators> operators =
                rechargeOperatporService.getAllActiveOperators();

        return ResponseEntity.ok(
                CommonResponse.success(operators, "operators fetch successfully")
        );
    }


    @GetMapping("/{type}")
    public ResponseEntity<CommonResponse<List<RechargeOperators>>> getByType(
            @PathVariable String type) {

        List<RechargeOperators> operators =
                rechargeOperatporService.getByType(type);

        return ResponseEntity.ok(
                CommonResponse.success(operators, "operators fetch successfully")
        );
    }
}
