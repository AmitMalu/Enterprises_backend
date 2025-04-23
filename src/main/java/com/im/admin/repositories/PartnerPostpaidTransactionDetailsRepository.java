package com.im.admin.repositories;


import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerPostpaidTransactionDetailsRepository extends JpaRepository<PartnerPostpaidTransactionDetails, Integer> {
}
