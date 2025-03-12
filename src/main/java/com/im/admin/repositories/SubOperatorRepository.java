package com.im.admin.repositories;


import com.im.admin.entities.SubOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubOperatorRepository extends JpaRepository<SubOperator, String> {

    boolean existsById(String id);
}

