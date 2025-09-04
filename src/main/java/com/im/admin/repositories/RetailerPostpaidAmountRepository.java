package com.im.admin.repositories;

import com.im.admin.entities.RetailerPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface
RetailerPostpaidAmountRepository extends JpaRepository<RetailerPostpaidAmount, Long> {

    Optional<RetailerPostpaidAmount> findByRetailerId_RetailerId(String retailerId);

}