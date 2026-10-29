package com.scs.admin.repositories;

import com.scs.admin.entities.RetailerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerPrepaidAmountRepository extends JpaRepository<RetailerPrepaidAmount, Long> {

    Optional<RetailerPrepaidAmount> findByRetailerId_RetailerId(String retailerId);

    @Query("""
    SELECT COALESCE(SUM(a.availableBalance), 0)
    FROM RetailerPrepaidAmount a
    WHERE a.retailerId.retailerId IN :ids
""")
    BigDecimal sumAvailableBalanceByRetailers(@Param("ids") List<String> ids);

}
