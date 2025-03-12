package com.im.admin.repositories;

import com.im.admin.entities.Operators;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorsRepository extends JpaRepository<Operators, String> {

    boolean existsById(String id);

    boolean existsByOperatorCode(String operatorCode);

    List<Operators> findBySubProductIdAndIsActiveTrue(String subProductId);

}

