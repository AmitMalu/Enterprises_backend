package com.im.admin.repositories;

import com.im.admin.entities.RetailerPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerPostpaidAmountRepository extends JpaRepository<RetailerPostpaidAmount, Long> {

    RetailerPostpaidAmount findByRetailerId_RetailerId(String retailerId);

}