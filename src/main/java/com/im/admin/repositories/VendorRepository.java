package com.im.admin.repositories;

import com.im.admin.entities.Operators;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendors, String>, JpaSpecificationExecutor<Vendors> {

    List<Vendors> findByProductIdAndActiveTrue(Short productId);

    boolean existsByVendorCode(String vendorCode);

    List<Vendors> findByActiveTrue();

    List<Vendors> findByProduct(Products product);

    @Query("""

            SELECT DISTINCT v
            FROM Vendors v
            JOIN VendorRouting vr
            ON (
                    vr.routingPriority1.id = v.id
                    OR vr.routingPriority2.id = v.id
                    OR vr.routingPriority3.id = v.id
            )
            JOIN vr.subProduct sp

            WHERE
                v.active = true
                AND
                sp.isWhiteLabelAdmin = true
                AND
                sp.whiteLabelAdminId = :wlId

            """)
    List<Vendors> findActiveVendorsForWL(
            @Param("wlId")
            String wlId
    );

}
