package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorPostpaidAmountRepository extends JpaRepository<DistributorPostpaidAmount, Long> {

    void deleteByDistributorId(Distributor distributorId);

}
