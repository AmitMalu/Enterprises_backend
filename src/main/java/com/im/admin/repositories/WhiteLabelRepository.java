package com.im.admin.repositories;

import com.im.admin.entities.Admin;
import com.im.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhiteLabelRepository extends JpaRepository<WhiteLabels, String> {

    boolean existsByAbbreviation(String abbreviation);

    List<WhiteLabels> findByStatus(String status);

    List<WhiteLabels> findByParentAccountContaining(String parentType);

    List<WhiteLabels> findByStatusIgnoreCase(String status);

    Optional<WhiteLabels> findByWhiteLabelId(String whiteLabelId);

}


