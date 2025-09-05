package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.SuperDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnterprisesRepository extends JpaRepository<Enterprises, String> {

    boolean existsByAbbreviation(String abbreviation);

    List<Enterprises> findByStatus(String status);


    List<Enterprises> findByParentAccountContaining(String parentType);

    List<Enterprises> findByStatusIgnoreCase(String status);

    Optional<Enterprises> findByEnterpriseId(String enterpriseId);

    @Query("SELECT e.name FROM Enterprises e WHERE e.enterpriseId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);


    List<Enterprises> findByParentAccount(String fromId);

   Enterprises findByMobileNumber(String mobileNumber);
}


