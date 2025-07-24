package com.im.admin.repositories;

import com.im.admin.entities.Operators;
import com.im.admin.entities.product.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorsRepository extends JpaRepository<Operators, String>, JpaSpecificationExecutor<Operators> {


    boolean existsById(String id);

    boolean existsByOperatorCode(String operatorCode);

    List<Operators> findBySubProductIdAndIsActiveTrue(String subProductId);

    List<Operators> findBySubProductCode(String subProductCode);

    Optional<Operators> findByOperatorName(String operatorName);

    Optional<Operators> findByCardBinNo(String cardBinNo);

    Optional<Operators> findByProductCodeAndSubProductCodeAndOperatorCode(
            String productCode, String subProductCode, String operatorCode
    );

    List<Operators> findByIsActiveTrue();

    Optional<Operators> findByOperatorCode(String operatorCode);

    List<Operators> findBySubProductId(String subProductId);


}

