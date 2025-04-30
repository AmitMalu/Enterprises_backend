package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhiteLabelRepository extends JpaRepository<WhiteLabels, String> {

    boolean existsByAbbreviation(String abbreviation);

}


