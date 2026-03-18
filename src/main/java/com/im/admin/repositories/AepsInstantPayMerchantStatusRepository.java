package com.im.admin.repositories;

import com.im.admin.entities.AepsInstantPayMerchantStatus;
import com.im.admin.entities.AepsMerchantStatus;
import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsInstantPayMerchantStatusRepository extends JpaRepository<AepsInstantPayMerchantStatus, Long> {

    Optional<AepsInstantPayMerchantStatus> findByRetailerId(String retailerId);

    Optional<AepsInstantPayMerchantStatus> findByRetailerIdAndVendor(String retailerId, Vendors vendor);
}
