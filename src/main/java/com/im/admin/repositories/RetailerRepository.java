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

    List<Retailer> findByParentAccountContainingIgnoreCase(String parentAccount);

    Retailer findByMobileNumber(String mobileNumber);

    //boolean existsByAbbreviation(String abbrivation);

    @Query("""
        SELECT r.retailerId
        FROM Retailer r
        WHERE r.parentAccount IN :distributorIds
    """)
    List<String> findRetailerIdsByDistributors(
            @Param("distributorIds") List<String> distributorIds
    );

    @Query("""
    SELECT COUNT(r)
    FROM Retailer r
    WHERE r.parentAccount IN :distributorIds
""")
    Long countRetailersByDistributors(
            @Param("distributorIds") List<String> distributorIds
    );

    long countByStatus(String status);
//    List<String> findRetailerIdsByParent(String hierarchyId);

}

