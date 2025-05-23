package com.im.admin.repositories;

import com.im.admin.entities.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerRepository extends JpaRepository<Retailer, String> {

    Optional<Retailer> findByRetailerId(String id);

    List<Retailer> findByStatus(String status);

    boolean existsByAbbreviation(String abbrivation);

}

