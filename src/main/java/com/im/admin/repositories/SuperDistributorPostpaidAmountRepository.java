package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperDistributorPostpaidAmountRepository extends JpaRepository<SuperDistributorPostpaidAmount, Long> {
}

