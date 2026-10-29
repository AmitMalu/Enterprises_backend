package com.scs.admin.repositories;

import com.scs.admin.entities.RetailerPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface
RetailerPostpaidAmountRepository extends JpaRepository<RetailerPostpaidAmount, Long> {

    Optional<RetailerPostpaidAmount> findByRetailerId_RetailerId(String retailerId);

    @Query("""
    SELECT COALESCE(SUM(r.availableBalance), 0)
    FROM RetailerPostpaidAmount r
    WHERE r.retailerId.retailerId IN :ids
""")
    BigDecimal sumAvailableBalanceByRetailers(@Param("ids") List<String> ids);

}