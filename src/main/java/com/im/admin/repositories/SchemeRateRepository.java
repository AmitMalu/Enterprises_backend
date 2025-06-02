package com.im.admin.repositories;

import com.im.admin.entities.SchemeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRateRepository extends JpaRepository<SchemeRate, Long> {

    List<SchemeRate> findByOperatorCode(String operatorCode);

    List<SchemeRate> findAllByOperatorNameAndOperatorCode(String operatorName, String operatorCode);

    SchemeRate findBySchemeCodeAndOperatorCode(String schemeCode, String operatorCode);


}
