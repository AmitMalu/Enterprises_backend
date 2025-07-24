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

    @Query("SELECT s FROM SchemeRate s WHERE s.operatorCode = :operatorCode")
    Optional<SchemeRate> findSchemeByOperatorCode(@Param("operatorCode") String operatorCode);

    @Query("SELECT s FROM SchemeRate s WHERE s.operatorName = :operatorName")
    List<SchemeRate> findSchemeByOperatorName(@Param("operatorName") String operatorName);

    List<SchemeRate> findBySchemeCodeAndOperatorName(String schemeCode, String operatorName);

    List<SchemeRate> findBySchemeCode(String schemeCode);


}
