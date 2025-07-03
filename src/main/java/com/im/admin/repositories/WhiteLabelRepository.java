package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhiteLabelRepository extends JpaRepository<WhiteLabels, String> {

    boolean existsByAbbreviation(String abbreviation);

    List<WhiteLabels> findByStatus(String status);

    List<WhiteLabels> findByParentAccountContaining(String parentType);
}


