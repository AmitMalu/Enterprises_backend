package com.im.admin.repositories;

import com.im.admin.entities.CallbackEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallbackEventLogRepository extends JpaRepository<CallbackEventLog, Integer> {

    @Query(value = "CALL callback_event_log_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getCallbackLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);


    Optional<CallbackEventLog> findTopByOrderIdOrderByIdDesc(String orderId);

    @Query(value = """
        SELECT *
        FROM callback_event_log
        WHERE request LIKE CONCAT('%ORDERID=', :txnId, '%')
        ORDER BY id DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<CallbackEventLog> findLatestByTxnIdInRequest(
            @Param("txnId") String txnId);

    @Query(value = """
    SELECT *
    FROM callback_event_log
    WHERE request LIKE CONCAT('%ORDERID=%', :txnPart, '%')
    ORDER BY id DESC
    LIMIT 1
    """, nativeQuery = true)
    Optional<CallbackEventLog> findLatestPgCallback(
            @Param("txnPart") String txnPart);

}
