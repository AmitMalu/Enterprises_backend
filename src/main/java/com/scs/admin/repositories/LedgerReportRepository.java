package com.scs.admin.repositories;


import com.scs.admin.entities.LedgerReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LedgerReportRepository extends JpaRepository<LedgerReport, String> {

    @Query(value = """
        SELECT * FROM ledger_report_view
        WHERE (:id IS NULL OR id LIKE %:id%)
        AND (:transactionId IS NULL OR transaction_id LIKE %:transactionId%)
        AND (:hierarchyId IS NULL OR hierarchy_id LIKE %:hierarchyId%)
        AND (:dateOfTransaction IS NULL OR date_of_transaction = :dateOfTransaction)
        AND (:fromDate IS NULL OR date_of_transaction >= :fromDate)
        AND (:toDate IS NULL OR date_of_transaction <= :toDate)
        AND (:timeOfTransaction IS NULL OR time_of_transaction = :timeOfTransaction)
        AND (:balBeforeTran IS NULL OR bal_before_tran = :balBeforeTran)
        AND (:amount IS NULL OR amount = :amount)
        AND (:balAfterTran IS NULL OR bal_after_tran = :balAfterTran)
        AND (:finalBalance IS NULL OR final_balance = :finalBalance)
        AND (:tranStatus IS NULL OR tran_status LIKE %:tranStatus%)
        AND (:actionOnBalanceAmount IS NULL OR action_on_balance_amount LIKE %:actionOnBalanceAmount%)
        AND (:service IS NULL OR service LIKE %:service%)
        AND (:mobileNo IS NULL OR mobile_no LIKE %:mobileNo%)
        AND (:subProductName IS NULL OR sub_product_name LIKE %:subProductName%)
        AND (:remarks IS NULL OR remarks LIKE %:remarks%)
        AND (:narration IS NULL OR narration LIKE %:narration%)
        AND (:sourceTable IS NULL OR source_table LIKE %:sourceTable%)
        ORDER BY date_of_transaction DESC, time_of_transaction DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM ledger_report_view
        WHERE (:id IS NULL OR id LIKE %:id%)
        AND (:transactionId IS NULL OR transaction_id LIKE %:transactionId%)
        AND (:hierarchyId IS NULL OR hierarchy_id LIKE %:hierarchyId%)
        AND (:dateOfTransaction IS NULL OR date_of_transaction = :dateOfTransaction)
        AND (:fromDate IS NULL OR date_of_transaction >= :fromDate)
        AND (:toDate IS NULL OR date_of_transaction <= :toDate)
        AND (:timeOfTransaction IS NULL OR time_of_transaction = :timeOfTransaction)
        AND (:balBeforeTran IS NULL OR bal_before_tran = :balBeforeTran)
        AND (:amount IS NULL OR amount = :amount)
        AND (:balAfterTran IS NULL OR bal_after_tran = :balAfterTran)
        AND (:finalBalance IS NULL OR final_balance = :finalBalance)
        AND (:tranStatus IS NULL OR tran_status LIKE %:tranStatus%)
        AND (:actionOnBalanceAmount IS NULL OR action_on_balance_amount LIKE %:actionOnBalanceAmount%)
        AND (:service IS NULL OR service LIKE %:service%)
        AND (:mobileNo IS NULL OR mobile_no LIKE %:mobileNo%)
        AND (:subProductName IS NULL OR sub_product_name LIKE %:subProductName%)
        AND (:remarks IS NULL OR remarks LIKE %:remarks%)
        AND (:narration IS NULL OR narration LIKE %:narration%)
        AND (:sourceTable IS NULL OR source_table LIKE %:sourceTable%)
        """,
            nativeQuery = true
    )
    Page<LedgerReport> findLedgerReportsWithFilters(
            @Param("id") String id,
            @Param("transactionId") String transactionId,
            @Param("hierarchyId") String hierarchyId,
            @Param("dateOfTransaction") LocalDate dateOfTransaction,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("timeOfTransaction") String timeOfTransaction,
            @Param("balBeforeTran") BigDecimal balBeforeTran,
            @Param("amount") BigDecimal amount,
            @Param("balAfterTran") BigDecimal balAfterTran,
            @Param("finalBalance") BigDecimal finalBalance,
            @Param("tranStatus") String tranStatus,
            @Param("actionOnBalanceAmount") String actionOnBalanceAmount,
            @Param("service") String service,
            @Param("mobileNo") String mobileNo,
            @Param("subProductName") String subProductName,
            @Param("remarks") String remarks,
            @Param("narration") String narration,
            @Param("sourceTable") String sourceTable,
            Pageable pageable
    );



    @Query("SELECT l FROM LedgerReport l " +
            "WHERE l.dateOfTransaction BETWEEN :fromDate AND :toDate " +
            "ORDER BY l.dateOfTransaction DESC, l.timeOfTransaction DESC")
    List<LedgerReport> findLedgerReportsForDownload(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
