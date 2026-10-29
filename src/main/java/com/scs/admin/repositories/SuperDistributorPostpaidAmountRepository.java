package com.scs.admin.repositories;

import com.scs.admin.entities.SuperDistributor;
import com.scs.admin.entities.SuperDistributorPostpaidAmount;
import com.scs.admin.entities.SuperDistributorPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperDistributorPostpaidAmountRepository extends JpaRepository<SuperDistributorPostpaidAmount, Long> {

    void deleteBySuperDistributorId(SuperDistributor superDistributor);

    Optional<SuperDistributorPostpaidAmount> findBySuperDistributorId_SuperDistributorId(String SuperDistributorId);


}

