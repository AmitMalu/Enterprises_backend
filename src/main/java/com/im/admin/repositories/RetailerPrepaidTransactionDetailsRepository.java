package com.im.admin.repositories;

import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import com.im.admin.response.DashboardTransactionDto;
import com.im.admin.service.MonthlyTransactionVolumeProjection;
import com.im.admin.service.ProductAmountProjection;
import com.im.admin.strategy.ProductCountProjection;
import com.im.admin.strategy.ProductVolumeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RetailerPrepaidTransactionDetailsRepository extends JpaRepository<RetailerPrepaidTransactionDetails, Integer>, BaseTransactionRepository<RetailerPrepaidTransactionDetails, Integer> {

    Optional<RetailerPrepaidTransactionDetails> findByTransactionId(String transactionId);

    List<RetailerPrepaidTransactionDetails> findByBaseTransactionId(String baseTxnId);

    @Query("""
                SELECT r FROM RetailerPrepaidTransactionDetails r
                WHERE (:id IS NULL OR r.id = :id)
                AND (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate)
                AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate)
                AND (:subProductNames IS NULL OR r.subProductName IN :subProductNames)
                AND (:vendorName IS NULL OR LOWER(r.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')))
                AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)
                AND (:networkIds IS NULL OR r.retailerId IN :networkIds)
                AND (r.transactionId LIKE 'TXN%')
                AND (:userId IS NULL OR LOWER(r.retailerId) LIKE LOWER(CONCAT('%', :userId, '%')))
                AND (:mobileNo IS NULL OR LOWER(r.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
                AND (:schemeName IS NULL OR LOWER(r.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
                AND (:amount IS NULL OR r.amount = :amount)
                AND (:txId IS NULL OR LOWER(r.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
                AND (:remarks IS NULL OR LOWER(r.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
                AND (:medium IS NULL OR LOWER(r.medium) LIKE LOWER(CONCAT('%', :medium, '%')))
                AND (:txType IS NULL OR LOWER(r.transactionType) LIKE LOWER(CONCAT('%', :txType, '%')))
                AND (:txStatus IS NULL OR LOWER(r.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
                AND (:dateOfTransaction IS NULL OR r.dateOfTransaction = :dateOfTransaction)
                AND (:timeOfTransaction IS NULL OR r.timeOfTransaction = :timeOfTransaction)
                AND (:updatedDateOfTransaction IS NULL OR r.updatedDateOfTransaction = :updatedDateOfTransaction)
                AND (:updatedTimeOfTransaction IS NULL OR r.updatedTimeOfTransaction = :updatedTimeOfTransaction)
            """)
    Page<RetailerPrepaidTransactionDetails> findFilteredTransactions(@Param("id") Integer id, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("subProductNames") List<String> subProductNames,  // List<String>
                                                                     @Param("vendorName") String vendorName, @Param("tranStatusList") List<String> tranStatusList, @Param("networkIds") List<String> networkIds, @Param("userId") String userId, @Param("mobileNo") String mobileNo, @Param("schemeName") String schemeName, @Param("amount") Double amount, @Param("txId") String txId, @Param("remarks") String remarks, @Param("medium") String medium, @Param("txType") String txType, @Param("txStatus") String txStatus, @Param("dateOfTransaction") LocalDate dateOfTransaction, @Param("timeOfTransaction") LocalTime timeOfTransaction, @Param("updatedDateOfTransaction") LocalDate updatedDateOfTransaction, @Param("updatedTimeOfTransaction") LocalTime updatedTimeOfTransaction, Pageable pageable);

    // Search by normalized transactionId + retailerId
    @Query("""
                SELECT t FROM RetailerPrepaidTransactionDetails t
                WHERE t.transactionId LIKE %:txnId%
                AND t.retailerId IN :networkIds
            """)
    List<RetailerPrepaidTransactionDetails> findTransactionsByTransactionIdAndRetailerId(@Param("txnId") String txnId, @Param("networkIds") List<String> networkIds);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT t FROM RetailerPrepaidTransactionDetails t " + "WHERE t.transactionId LIKE %:txnId%")
    List<RetailerPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);


    @Query("""
                SELECT new com.im.admin.response.DashboardTransactionDto(
                    r.dateOfTransaction,
                    r.timeOfTransaction,
                    r.amount,
                    p.name,
                    sp.walletType,
                    r.tranStatus
                )
                FROM RetailerPrepaidTransactionDetails r
                LEFT JOIN SubProducts sp ON sp.subProductTitle = r.subProductName
                LEFT JOIN sp.product p
                WHERE r.retailerId = :retailerId
                ORDER BY r.dateOfTransaction DESC, r.timeOfTransaction DESC
            """)
    List<DashboardTransactionDto> fetchDashboardTransactions(@Param("retailerId") String retailerId);


    @Query("""
                SELECT COALESCE(SUM(t.amount), 0)
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId = :retailerId
                  AND t.tranStatus = 'SUCCESS'
                  AND t.transactionId LIKE 'TXN%'
                  AND t.dateOfTransaction BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumTransactionVolume(@Param("retailerId") String retailerId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Query("""
                SELECT COALESCE(SUM(r.amount), 0)
                FROM RetailerPrepaidTransactionDetails r
                WHERE r.retailerId = :retailerId
                  AND r.transactionId LIKE 'COM%'
                  AND r.tranStatus = 'SUCCESS'
                  AND (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate)
                  AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate)
            """)
    BigDecimal sumCommissionEarned(@Param("retailerId") String retailerId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("""
                SELECT COUNT(t)
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId = :retailerId
                  AND t.tranStatus = 'PENDING'
                  AND (:fromDate IS NULL OR t.dateOfTransaction >= :fromDate)
                  AND (:toDate IS NULL OR t.dateOfTransaction <= :toDate)
            """)
    Long countPendingTransactions(@Param("retailerId") String retailerId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Query("""
                SELECT 
                    FUNCTION('DATE_FORMAT', t.dateOfTransaction, '%Y-%m') AS month,
                    COALESCE(SUM(t.amount), 0) AS value
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId = :retailerId
                  AND t.tranStatus = 'SUCCESS'
                  AND t.transactionId LIKE 'TXN%'
                  AND t.dateOfTransaction >= :fromDate
                GROUP BY FUNCTION('DATE_FORMAT', t.dateOfTransaction, '%Y-%m')
                ORDER BY month
            """)
    List<MonthlyTransactionVolumeProjection> getLast4MonthsTransactionVolume(@Param("retailerId") String retailerId, @Param("fromDate") LocalDate fromDate);


    @Query("""
                SELECT
                    p.name AS productName,
                    COALESCE(SUM(t.amount), 0) AS amount
                FROM RetailerPrepaidTransactionDetails t
                JOIN SubProducts sp ON sp.subProductCode = t.subProductName
                JOIN Products p ON p.id = sp.product.id
                WHERE t.retailerId = :retailerId
                  AND t.tranStatus = 'SUCCESS'
                  AND t.transactionId LIKE 'TXN%'
                  AND MONTH(t.dateOfTransaction) = MONTH(CURRENT_DATE)
                  AND YEAR(t.dateOfTransaction) = YEAR(CURRENT_DATE)
                GROUP BY p.name
            """)
    List<ProductAmountProjection> getThisMonthProductWiseAmount(@Param("retailerId") String retailerId);

    @Modifying
    @Query("""
                DELETE FROM RetailerPrepaidTransactionDetails t
                WHERE t.transactionId = :txnId
                  AND t.actionOnBalanceAmount LIKE 'Settlement Pending%'
            """)
    void deleteSettlementPendingRecord(@Param("txnId") String txnId);


    @Query("""
                SELECT r FROM RetailerPrepaidTransactionDetails r
                WHERE (:id IS NULL OR r.id = :id)
                AND (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate)
                AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate)
                AND (:subProductNames IS NULL OR r.subProductName IN :subProductNames)
                AND (:vendorName IS NULL OR LOWER(r.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')))
                AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)
                AND (:userId IS NULL OR LOWER(r.retailerId) LIKE LOWER(CONCAT('%', :userId, '%')))
                AND (:mobileNo IS NULL OR LOWER(r.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
                AND (:schemeName IS NULL OR LOWER(r.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
                AND (:amount IS NULL OR r.amount = :amount)
                AND (:txId IS NULL OR LOWER(r.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
                AND (:remarks IS NULL OR LOWER(r.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
                AND (:medium IS NULL OR LOWER(r.medium) LIKE LOWER(CONCAT('%', :medium, '%')))
                AND (:txType IS NULL OR LOWER(r.transactionType) LIKE LOWER(CONCAT('%', :txType, '%')))
                AND (:txStatus IS NULL OR LOWER(r.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
            
                AND (
                                :actionOnBalanceAmount IS NULL
                                OR LOWER(TRIM(r.actionOnBalanceAmount)) LIKE
                                   CONCAT(LOWER(TRIM(:actionOnBalanceAmount)), '%')
                            )
                            AND r.tranStatus <> 'FAILURE'
            
                AND (:dateOfTransaction IS NULL OR r.dateOfTransaction = :dateOfTransaction)
                AND (:timeOfTransaction IS NULL OR r.timeOfTransaction = :timeOfTransaction)
                AND (:updatedDateOfTransaction IS NULL OR r.updatedDateOfTransaction = :updatedDateOfTransaction)
                AND (:updatedTimeOfTransaction IS NULL OR r.updatedTimeOfTransaction = :updatedTimeOfTransaction)
            """)
    Page<RetailerPrepaidTransactionDetails> findFilteredTransactionsUpdated(@Param("id") Integer id, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("subProductNames") List<String> subProductNames,  // List<String>
                                                                            @Param("vendorName") String vendorName, @Param("tranStatusList") List<String> tranStatusList, @Param("userId") String userId, @Param("mobileNo") String mobileNo, @Param("schemeName") String schemeName, @Param("amount") Double amount, @Param("txId") String txId, @Param("remarks") String remarks, @Param("medium") String medium, @Param("txType") String txType, @Param("txStatus") String txStatus, @Param("actionOnBalanceAmount") String actionOnBalanceAmount, @Param("dateOfTransaction") LocalDate dateOfTransaction, @Param("timeOfTransaction") LocalTime timeOfTransaction, @Param("updatedDateOfTransaction") LocalDate updatedDateOfTransaction, @Param("updatedTimeOfTransaction") LocalTime updatedTimeOfTransaction, Pageable pageable);

    @Query("""
                SELECT COALESCE(SUM(t.amount), 0)
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId IN :retailerIds
                  AND t.tranStatus = 'SUCCESS'
                  AND t.transactionId LIKE 'TXN%'
                  AND t.dateOfTransaction BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumTransactionVolumeForRetailers(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("""
                SELECT COUNT(t)
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId IN :retailerIds
                  AND t.tranStatus = 'PENDING'
                  AND (:fromDate IS NULL OR t.dateOfTransaction >= :fromDate)
                  AND (:toDate IS NULL OR t.dateOfTransaction <= :toDate)
            """)
    Long countPendingTransactionsForRetailers(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Query("""
                SELECT 
                    FUNCTION('DATE_FORMAT', t.dateOfTransaction, '%Y-%m') AS month,
                    COALESCE(SUM(t.amount), 0) AS value
                FROM RetailerPrepaidTransactionDetails t
                WHERE t.retailerId IN :retailerIds
                  AND t.tranStatus = 'SUCCESS'
                  AND t.transactionId LIKE 'TXN%'
                  AND t.dateOfTransaction >= :fromDate
                GROUP BY FUNCTION('DATE_FORMAT', t.dateOfTransaction, '%Y-%m')
                ORDER BY month
            """)
    List<MonthlyTransactionVolumeProjection> getLast4MonthsTransactionVolumeForRetailers(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate);


    @Query("""
                SELECT r FROM RetailerPrepaidTransactionDetails r
                WHERE (:id IS NULL OR r.id = :id)
                AND (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate)
                AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate)
                AND (:subProductNames IS NULL OR r.subProductName IN :subProductNames)
                AND (:vendorName IS NULL OR LOWER(r.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%')))
                AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)
                 AND (:networkIds IS NULL OR r.retailerId IN :networkIds)
                AND (:userId IS NULL OR LOWER(r.retailerId) LIKE LOWER(CONCAT('%', :userId, '%')))
                AND (:mobileNo IS NULL OR LOWER(r.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
                AND (:schemeName IS NULL OR LOWER(r.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
                AND (:amount IS NULL OR r.amount = :amount)
            
                  AND r.transactionId LIKE 'TXN%'
                   AND (:txId IS NULL OR LOWER(r.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
            
            
                AND (:remarks IS NULL OR LOWER(r.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
                AND (:medium IS NULL OR LOWER(r.medium) LIKE LOWER(CONCAT('%', :medium, '%')))
                AND (:txType IS NULL OR LOWER(r.transactionType) LIKE LOWER(CONCAT('%', :txType, '%')))
            
                 AND (
                    :actionOnBalanceAmount IS NULL
                    OR LOWER(r.actionOnBalanceAmount) LIKE
                       CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%')
                  )
                AND (:txStatus IS NULL OR LOWER(r.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
                AND (:dateOfTransaction IS NULL OR r.dateOfTransaction = :dateOfTransaction)
              AND (
                              :timeOfTransaction IS NULL OR\s
                              CAST(r.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%')
                          )
                AND (:updatedDateOfTransaction IS NULL OR r.updatedDateOfTransaction = :updatedDateOfTransaction)
                AND (:updatedTimeOfTransaction IS NULL OR r.updatedTimeOfTransaction = :updatedTimeOfTransaction)
            """)
    Page<RetailerPrepaidTransactionDetails> findFilteredTransactionTXN(  //..
                                                                         @Param("id") Integer id, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("subProductNames") List<String> subProductNames,  // List<String>
                                                                         @Param("vendorName") String vendorName, @Param("tranStatusList") List<String> tranStatusList, @Param("networkIds") List<String> networkIds, @Param("userId") String userId, @Param("mobileNo") String mobileNo, @Param("schemeName") String schemeName, @Param("amount") Double amount, @Param("txId") String txId, @Param("remarks") String remarks, @Param("medium") String medium, @Param("txType") String txType, @Param("actionOnBalanceAmount") String actionOnBalanceAmount, @Param("txStatus") String txStatus, @Param("dateOfTransaction") LocalDate dateOfTransaction, @Param("timeOfTransaction") LocalTime timeOfTransaction, @Param("updatedDateOfTransaction") LocalDate updatedDateOfTransaction, @Param("updatedTimeOfTransaction") LocalTime updatedTimeOfTransaction, Pageable pageable);

    @Query("""
                SELECT sp.product.name AS productName,
                       COALESCE(SUM(r.amount), 0) AS totalAmount
                FROM RetailerPrepaidTransactionDetails r
                JOIN SubProducts sp ON r.subProductName = sp.subProductTitle
                WHERE r.retailerId IN :retailerIds
                  AND r.tranStatus = 'SUCCESS'
                  AND r.transactionId LIKE 'TXN%'
                  AND r.dateOfTransaction BETWEEN :fromDate AND :toDate
                GROUP BY sp.product.name
            """)
    List<ProductVolumeProjection> getProductWiseVolumeForRetailers(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Query("""
                SELECT sp.product.name AS productName,
                       COUNT(r.id) AS transactionCount
                FROM RetailerPrepaidTransactionDetails r
                JOIN SubProducts sp ON r.subProductName = sp.subProductTitle
                WHERE r.retailerId IN :retailerIds
                  AND r.tranStatus = 'SUCCESS'
                  AND r.transactionId LIKE 'TXN%'
                  AND r.dateOfTransaction BETWEEN :fromDate AND :toDate
                GROUP BY sp.product.name
            """)
    List<ProductCountProjection> getProductWiseTransactionCount(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("""
                SELECT COUNT(r)
                FROM RetailerPrepaidTransactionDetails r
                WHERE r.retailerId IN :retailerIds
                  AND r.tranStatus = 'PENDING'
                  AND r.dateOfTransaction BETWEEN :fromDate AND :toDate
            """)
    Long getPendingTransactionCount(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(value = """
                SELECT p.partner_id AS id,
                        COALESCE(SUM(t.amount), 0) AS totalBusiness
                FROM retailer_prepaid_transaction_details t
                JOIN retailer r ON t.retailer_id = r.retailer_id
                JOIN partner p ON r.partner_id = p.partner_id
                WHERE t.retailer_id IN (:retailerIds)
                   AND (:fromDate IS NULL OR t.date_of_transaction >= :fromDate)
                  AND (:toDate IS NULL OR t.date_of_transaction <= :toDate)
                GROUP BY p.partner_id
            """, nativeQuery = true)
    List<Map<String, Object>> getPartnerBusinessPrepaid(@Param("retailerIds") List<String> retailerIds, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    boolean existsByTransactionId(String transactionId);

}




