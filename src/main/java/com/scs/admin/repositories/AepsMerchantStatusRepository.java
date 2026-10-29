package com.scs.admin.repositories;

import com.scs.admin.entities.AepsMerchantStatus;
import com.scs.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsMerchantStatusRepository extends JpaRepository<AepsMerchantStatus, Long> {

    Optional<AepsMerchantStatus> findByRetailerId(String retailerId);

    Optional<AepsMerchantStatus> findByRetailerIdAndVendor(String retailerId, Vendors vendor);
}
