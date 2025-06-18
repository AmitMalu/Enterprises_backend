package com.im.admin.repositories;


import com.im.admin.entities.SubOperator;
import com.im.admin.entities.SubProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubOperatorRepository extends JpaRepository<SubOperator, String>, JpaSpecificationExecutor<SubOperator>  {

    boolean existsById(String id);

    Optional<SubOperator> findBySubOperatorName(String name);

}

