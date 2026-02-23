package com.im.admin.repositories;

import com.im.admin.entities.AepsBalanceEnquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsBalanceEnquiryRepository extends JpaRepository<AepsBalanceEnquiry,Long> {
    Optional<AepsBalanceEnquiry> findByTransactionId(String transactionId);
}
