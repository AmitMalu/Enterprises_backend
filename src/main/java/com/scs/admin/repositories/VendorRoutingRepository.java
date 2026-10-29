package com.scs.admin.repositories;

import com.scs.admin.entities.SubProducts;
import com.scs.admin.entities.VendorRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRoutingRepository extends JpaRepository<VendorRouting, Long>, JpaSpecificationExecutor<VendorRouting> {

    @Query("""
                SELECT vr FROM VendorRouting vr
                LEFT JOIN FETCH vr.routingPriority1 r1
                LEFT JOIN FETCH r1.product
                LEFT JOIN FETCH vr.routingPriority2 r2
                LEFT JOIN FETCH r2.product
                LEFT JOIN FETCH vr.routingPriority3 r3
                LEFT JOIN FETCH r3.product
                JOIN FETCH vr.subProduct sp
                WHERE sp.id = :subProductId
            """)
    VendorRouting findBySubProductId(@Param("subProductId") String subProductId);


    VendorRouting findBySubProduct(SubProducts subProducts);
}
