package com.scs.admin.repositories;

import com.scs.admin.entities.VendorCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorCredentialsRepository extends JpaRepository<VendorCredentials, Long>, JpaSpecificationExecutor<VendorCredentials> {

    Optional<VendorCredentials> findByVendorIdAndProductId(String vendorId, Short productId);

    Optional<VendorCredentials> findByVendor_Id(String vendorId);

}

