package com.im.admin.repositories;

import com.im.admin.entities.Retailer;
import com.im.admin.entities.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerRepository extends JpaRepository<Retailer, String> {

    Optional<Retailer> findByRetailerId(String id);

    List<Retailer> findByStatus(String status);

    List<Retailer> findByParentAccountContaining(String parentType);

    List<Retailer> findByStatusIgnoreCase(String status);

    @Query("SELECT r.name FROM Retailer r WHERE r.retailerId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);

    List<Retailer> findByParentAccount(String fromId);



    //boolean existsByAbbreviation(String abbrivation);

}

