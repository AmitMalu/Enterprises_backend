package com.scs.admin.repositories;

import com.scs.admin.entities.FundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {

    @Query("""
       SELECT COALESCE(SUM(f.amount), 0)
       FROM FundTransfer f
       WHERE f.fromHierarchyId = :hierarchyId
       AND (:fromDate IS NULL OR f.transferDate >= :fromDate)
       AND (:toDate IS NULL OR f.transferDate <= :toDate)
       """)
    BigDecimal sumFundTransferred(
            @Param("hierarchyId") String hierarchyId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime  toDate
    );


}
