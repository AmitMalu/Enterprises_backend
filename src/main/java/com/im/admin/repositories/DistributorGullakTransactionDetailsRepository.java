package com.im.admin.repositories;

import com.im.admin.entities.DistributorGullakTransactionDetails;
import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorGullakTransactionDetailsRepository extends JpaRepository<DistributorGullakTransactionDetails, Integer> {


}
