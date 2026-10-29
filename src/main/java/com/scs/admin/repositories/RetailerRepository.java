package com.scs.admin.repositories;

import com.scs.admin.entities.Retailer;
import com.scs.admin.entities.SuperAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerRepository extends JpaRepository<Retailer, String> {

    Optional<Retailer> findByRetailerId(String id);

    @Query("SELECT r.name FROM Retailer r WHERE r.retailerId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);

    List<Retailer> findByStatus(String status);

    List<Retailer> findByStatusIgnoreCase(String status);

    List<Retailer> findByParentAccount(String fromId);

    List<Retailer> findByParentAccountContainingIgnoreCase(String parentAccount);

    Retailer findByMobileNumber(String mobileNumber);

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

    Page<Retailer>
    findByRetailerIdIn(
            List<String> ids,
            Pageable pageable
    );

}

