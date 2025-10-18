package com.im.admin.repositories;

import com.im.admin.entities.SchemeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRateRepository extends JpaRepository<SchemeRate, Long> {

    List<SchemeRate> findByOperatorCode(String operatorCode);

    List<SchemeRate> findAllByOperatorNameAndOperatorCode(String operatorName, String operatorCode);

    SchemeRate findBySchemeCodeAndOperatorCode(String schemeCode, String operatorCode);

    //    @Query("SELECT s FROM SchemeRate s WHERE s.operatorCode = :operatorCode")
//    Optional<SchemeRate> findSchemeByOperatorCode(@Param("operatorCode") String operatorCode);

    @Query("SELECT s FROM SchemeRate s WHERE s.operatorCode = :operatorCode")
    List<SchemeRate> findSchemeByOperatorCode(@Param("operatorCode") String operatorCode);

    @Query("SELECT s FROM SchemeRate s WHERE s.operatorName = :operatorName")
    List<SchemeRate> findSchemeByOperatorName(@Param("operatorName") String operatorName);

    List<SchemeRate> findBySchemeCodeAndOperatorName(String schemeCode, String operatorName);

    List<SchemeRate> findBySchemeCode(String schemeCode);

    @Query("SELECT sr FROM SchemeRate sr " +
            "WHERE sr.operatorName = :operatorName " +
            //"AND sr.slabType = 'VARIABLE' " +
            "AND :amount BETWEEN sr.slabStart AND sr.slabEnd")
    Optional<SchemeRate> findVariableSlabByOperatorAndAmount(@Param("operatorName") String operatorName,
                                                             @Param("amount") Double amount);

    // Optional: For FIXED slabType
    @Query("SELECT sr FROM SchemeRate sr " +
            "WHERE sr.operatorName = :operatorName " +
            "AND sr.slabType = 'FIXED' " +
            "AND sr.fixedSlabValue <= :amount")
    Optional<SchemeRate> findFixedSlabByOperatorAndAmount(@Param("operatorName") String operatorName,
                                                          @Param("amount") Double amount);

    @Query("""
                SELECT sr FROM SchemeRate sr
                WHERE sr.schemeCode = :schemeCode
                  AND sr.operatorName = :operatorName
                  AND (
                        (sr.slabType = 'FIXED' AND :amount = sr.fixedSlabValue)
                     OR (sr.slabType = 'VARIABLE' AND :amount BETWEEN sr.slabStart AND sr.slabEnd)
                  )
            """)
    Optional<SchemeRate> findSchemeRateByCodeAndOperatorAndAmount(
            @Param("schemeCode") String schemeCode,
            @Param("operatorName") String operatorName,
            @Param("amount") Double amount
    );


}
