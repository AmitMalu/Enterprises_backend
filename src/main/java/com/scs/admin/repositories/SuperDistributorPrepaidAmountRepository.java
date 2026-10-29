package com.scs.admin.repositories;

import com.scs.admin.entities.SuperDistributor;
import com.scs.admin.entities.SuperDistributorPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperDistributorPrepaidAmountRepository extends JpaRepository<SuperDistributorPrepaidAmount, Long> {

    void deleteBySuperDistributorId(SuperDistributor superDistributor);

    Optional<SuperDistributorPrepaidAmount> findBySuperDistributorId_SuperDistributorId(String SuperDistributorId);
}

