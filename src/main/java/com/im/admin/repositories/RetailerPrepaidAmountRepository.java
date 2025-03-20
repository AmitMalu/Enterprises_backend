package com.im.admin.repositories;

import com.im.admin.entities.RetailerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerPrepaidAmountRepository extends JpaRepository<RetailerPrepaidAmount, Long> {

    RetailerPrepaidAmount findByRetailerId_RetailerId(String retailerId);

}
