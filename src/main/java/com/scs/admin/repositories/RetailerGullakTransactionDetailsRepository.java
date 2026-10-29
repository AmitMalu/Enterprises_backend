package com.scs.admin.repositories;

import com.scs.admin.entities.RetailerGullakTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerGullakTransactionDetailsRepository extends JpaRepository<RetailerGullakTransactionDetails, Integer> {


}
