package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT d.name FROM Distributor d WHERE d.distributorId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);


    List<Distributor> findByParentAccount(String fromId);

    List<Distributor> findByParentAccountContainingIgnoreCase(String fromId);

    Distributor findByMobileNumber(String mobileNumber);

    @Query("""
        SELECT d.distributorId
        FROM Distributor d
        WHERE d.parentAccount IN :partnerIds
    """)
    List<String> findDistributorIdsByPartners(
            @Param("partnerIds") List<String> partnerIds
    );

}

