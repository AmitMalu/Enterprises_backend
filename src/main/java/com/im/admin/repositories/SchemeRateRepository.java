package com.im.admin.repositories;

import com.im.admin.entities.SchemeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeRateRepository extends JpaRepository<SchemeRate, Long> {

    List<SchemeRate> findByOperatorCode(String operatorCode);

}

