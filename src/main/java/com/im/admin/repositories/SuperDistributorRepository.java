package com.im.admin.repositories;

import com.im.admin.entities.Partner;
import com.im.admin.entities.SuperDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuperDistributorRepository extends JpaRepository<SuperDistributor, String> {

    List<SuperDistributor> findByStatus(String status);

    List<SuperDistributor> findByParentAccountContaining(String parentType);

    List<SuperDistributor> findByStatusIgnoreCase(String status);

    //boolean existsByAbbreviation(String abbreviation);

    Optional<SuperDistributor> findBySuperDistributorId(String superDistributorId);


}

