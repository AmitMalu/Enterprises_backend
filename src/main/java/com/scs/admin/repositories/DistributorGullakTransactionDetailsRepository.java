package com.scs.admin.repositories;

import com.scs.admin.entities.DistributorGullakTransactionDetails;
import com.scs.admin.entities.DistributorPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorGullakTransactionDetailsRepository extends JpaRepository<DistributorGullakTransactionDetails, Integer> {


}
