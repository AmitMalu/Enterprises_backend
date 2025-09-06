package com.im.admin.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@NoRepositoryBean
public interface BaseTransactionRepository<T, ID> extends JpaRepository<T, ID> {

    @Query("SELECT t FROM #{#entityName} t " +
            "WHERE DATE(t.dateOfTransaction) = :date " +
            "AND HOUR(t.timeOfTransaction) = :hour")
    List<T> findByDateAndHour(@Param("date") LocalDate date, @Param("hour") int hour);

}

