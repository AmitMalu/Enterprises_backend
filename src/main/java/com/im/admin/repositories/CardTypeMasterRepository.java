package com.im.admin.repositories;

import com.im.admin.entities.CardTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CardTypeMasterRepository extends JpaRepository<CardTypeMaster, Long> {

    Optional<CardTypeMaster> findByCardBinNo(String cardBinNo);

    boolean existsByCardBinNo(String cardBinNo);

}
