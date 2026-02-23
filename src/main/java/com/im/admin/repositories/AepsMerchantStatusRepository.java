package com.im.admin.repositories;

import com.im.admin.entities.AepsMerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsMerchantStatusRepository extends JpaRepository<AepsMerchantStatus, Long> {

    Optional<AepsMerchantStatus> findByRetailerId(String retailerId);
}
