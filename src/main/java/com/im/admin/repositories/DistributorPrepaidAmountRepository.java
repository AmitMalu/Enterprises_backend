package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPrepaidAmount;
import com.im.admin.entities.RetailerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorPrepaidAmountRepository extends JpaRepository<DistributorPrepaidAmount, Long> {

    void deleteByDistributorId(Distributor distributorId);

    Optional<DistributorPrepaidAmount> findByDistributorId_DistributorId(String distributorId);

}
