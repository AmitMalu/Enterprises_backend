package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, String> {

    List<Distributor> findByStatus(String status);

    List<Distributor> findByParentAccountContaining(String parentType);


    List<Distributor> findByStatusIgnoreCase(String status);

    //boolean existsByAbbreviation(String abbreviation);

    Optional<Distributor> findByDistributorId(String distributorId);


}

