package com.im.admin.repositories;

import com.im.admin.entities.Partner;
import com.im.admin.entities.SuperDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT sd.name FROM SuperDistributor sd WHERE sd.superDistributorId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);


    List<SuperDistributor> findByParentAccount(String fromId);

    List<SuperDistributor> findByParentAccountContainingIgnoreCase(String fromId);

}

