package com.scs.admin.repositories;

import com.scs.admin.entities.SchemeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRateRepository extends JpaRepository<SchemeRate, Long> {

    List<SchemeRate> findBySchemeCode(String schemeCode);

    List<SchemeRate> findByOperatorCode(String operatorCode);

    List<SchemeRate> findAllByOperatorNameAndOperatorCode(String operatorName, String operatorCode);

    SchemeRate findBySchemeCodeAndOperatorCode(String schemeCode, String operatorCode);

    List<SchemeRate> findBySchemeCodeAndOperatorName(String schemeCode, String operatorName);

    @Query("SELECT sr FROM SchemeRate sr " + "WHERE sr.operatorName = :operatorName " +
            //"AND sr.slabType = 'VARIABLE' " +
            "AND :amount BETWEEN sr.slabStart AND sr.slabEnd")
    Optional<SchemeRate> findVariableSlabByOperatorAndAmount(@Param("operatorName") String operatorName, @Param("amount") Double amount);

    @Query("""
                SELECT sr FROM SchemeRate sr
                WHERE sr.schemeCode = :schemeCode
                  AND LOWER(sr.operatorName) = LOWER(:operatorName)
                  AND (:amount BETWEEN sr.slabStart AND sr.slabEnd)
            """)
    Optional<SchemeRate> findSchemeRateByCodeAndOperatorAndAmount(
            @Param("schemeCode") String schemeCode,
            @Param("operatorName") String operatorName,
            @Param("amount") Double amount
    );

    @Query("""
       SELECT s
       FROM SchemeRate s
       WHERE s.schemeCode = :schemeCode
       AND :amount BETWEEN s.slabStart AND s.slabEnd
       """)
    Optional<SchemeRate> findMatchingSlab(
            @Param("schemeCode") String schemeCode,
            @Param("amount") Double amount);

}
