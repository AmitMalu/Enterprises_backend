package com.scs.admin.repositories;

import com.scs.admin.entities.AepsInstantPayMerchantStatus;
import com.scs.admin.entities.AepsMerchantStatus;
import com.scs.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsInstantPayMerchantStatusRepository extends JpaRepository<AepsInstantPayMerchantStatus, Long> {

    Optional<AepsInstantPayMerchantStatus> findByRetailerId(String retailerId);

    Optional<AepsInstantPayMerchantStatus> findByRetailerIdAndVendor(String retailerId, Vendors vendor);
}
