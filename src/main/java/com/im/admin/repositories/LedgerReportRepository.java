package com.im.admin.repositories;


import com.im.admin.entities.LedgerReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LedgerReportRepository extends JpaRepository<LedgerReport, String> {

    @Query(value = """
        SELECT * FROM ledger_report_view
        WHERE date_of_transaction BETWEEN :fromDate AND :toDate
        ORDER BY date_of_transaction DESC, time_of_transaction DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM ledger_report_view
        WHERE date_of_transaction BETWEEN :fromDate AND :toDate
        """,
            nativeQuery = true)
    Page<LedgerReport> findLedgerReports(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);


    @Query("SELECT l FROM LedgerReport l " +
            "WHERE l.dateOfTransaction BETWEEN :fromDate AND :toDate " +
            "ORDER BY l.dateOfTransaction DESC, l.timeOfTransaction DESC")
    List<LedgerReport> findLedgerReportsForDownload(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
