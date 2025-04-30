package com.im.admin.repositories;

import com.im.admin.entities.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface CardTypeRepository extends JpaRepository<CardType, Long> {

    //Optional<CardType> findByCardBinNo(String cardBinNo);
}

