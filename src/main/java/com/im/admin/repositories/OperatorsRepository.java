package com.im.admin.repositories;

import com.im.admin.entities.Operators;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorsRepository extends JpaRepository<Operators, String> {

    boolean existsById(String id);

    boolean existsByOperatorCode(String operatorCode);

    List<Operators> findBySubProductIdAndIsActiveTrue(Short subProductId);

    List<Operators> findBySubProductCode(String subProductCode);

    Optional<Operators> findByOperatorName(String operatorName);

    Optional<Operators> findByCardBinNo(String cardBinNo);

    // OperatorsRepository.java
    Optional<Operators> findByProductCodeAndSubProductCodeAndOperatorCode(
            String productCode, String subProductCode, String operatorCode
    );


}

