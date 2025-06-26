package com.im.admin.repositories;

import com.im.admin.entities.PrefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrefundRequestRepository extends JpaRepository<PrefundRequest, Long>, JpaSpecificationExecutor<PrefundRequest> {

    @Query("SELECT w FROM PrefundRequest w WHERE w.bankTranId = :bankTranId AND w.status = 'APPROVED'")
    Optional<PrefundRequest> findApprovedByBankTranId(@Param("bankTranId") String bankTranId);

    @Query("SELECT w FROM PrefundRequest w WHERE w.bankTranId = :bankTranId AND w.depositAmount = :amount AND w.createDateTime >= :thresholdTime")
    List<PrefundRequest> findDuplicatesWithinTime(
            @Param("bankTranId") String bankTranId,
            @Param("amount") double amount,
            @Param("thresholdTime") LocalDateTime thresholdTime);

}
