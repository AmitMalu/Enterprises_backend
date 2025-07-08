package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.SuperDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnterprisesRepository extends JpaRepository<Enterprises, String> {

    boolean existsByAbbreviation(String abbreviation);

    List<Enterprises> findByStatus(String status);


    List<Enterprises> findByParentAccountContaining(String parentType);

    List<Enterprises> findByStatusIgnoreCase(String status);
}


