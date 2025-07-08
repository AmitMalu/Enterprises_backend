package com.im.admin.repositories;

import com.im.admin.entities.Partner;
import com.im.admin.entities.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, String> {

    //boolean existsByAbbreviation(String abbreviation);

    List<Partner> findByStatus(String status);

    List<Partner> findByParentAccountContaining(String parentType);


    List<Partner> findByStatusIgnoreCase(String status);
}

