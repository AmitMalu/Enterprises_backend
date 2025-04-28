package com.im.admin.repositories;

import com.im.admin.entities.FundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FundRequestRepository extends JpaRepository<FundRequest, Long> {
}
