package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperDistributorPrepaidAmountRepository extends JpaRepository<SuperDistributorPrepaidAmount, Long> {
}

