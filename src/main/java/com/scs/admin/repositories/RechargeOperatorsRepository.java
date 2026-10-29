package com.scs.admin.repositories;

import com.scs.admin.entities.RechargeOperators;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RechargeOperatorsRepository  extends JpaRepository<RechargeOperators, Long> {

    List<RechargeOperators> findByActiveTrue();

    List<RechargeOperators> findByTypeAndActiveTrue(String type);

    Optional<RechargeOperators> findByCodeAndType(String code, String type);

}
