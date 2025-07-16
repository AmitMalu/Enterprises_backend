package com.im.admin.repositories;

import com.im.admin.entities.VendorCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorCredentialsRepository extends JpaRepository<VendorCredentials, Long>, JpaSpecificationExecutor<VendorCredentials> {


}

