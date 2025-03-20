package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorPrepaidAmountRepository extends JpaRepository<DistributorPrepaidAmount, Long> {

    void deleteByDistributorId(Distributor distributorId);

}
