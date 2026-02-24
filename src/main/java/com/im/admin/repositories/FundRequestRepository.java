package com.im.admin.repositories;

import com.im.admin.entities.FundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface FundRequestRepository extends JpaRepository<FundRequest, Long> {

    @Query("""
       SELECT COALESCE(SUM(f.requestedAmount), 0)
       FROM FundRequest f
       WHERE f.requesterHierarchyId = :hierarchyId
       AND f.status = 'APPROVED'
       AND (:fromDate IS NULL OR f.requestRaiseDate >= :fromDate)
       AND (:toDate IS NULL OR f.requestRaiseDate <= :toDate)
       """)
    BigDecimal sumFundReceived(
            @Param("hierarchyId") String hierarchyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
