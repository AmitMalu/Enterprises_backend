package com.im.admin.repositories;

import com.im.admin.entities.RechargeOperators;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargeOperatorsRepository  extends JpaRepository<RechargeOperators, Long> {

    List<RechargeOperators> findByActiveTrue();

    List<RechargeOperators> findByTypeAndActiveTrue(String type);
}
