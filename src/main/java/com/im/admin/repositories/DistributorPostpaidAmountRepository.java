package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPostpaidAmount;
import com.im.admin.entities.DistributorPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorPostpaidAmountRepository extends JpaRepository<DistributorPostpaidAmount, Long> {

    void deleteByDistributorId(Distributor distributorId);

    Optional<DistributorPostpaidAmount> findByDistributorId_DistributorId(String distributorId);

}
