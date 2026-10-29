package com.scs.admin.repositories;

import com.scs.admin.entities.AepsOutletLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsOutletLoginLogRepository extends JpaRepository<AepsOutletLoginLog,Long>{
    Optional<AepsOutletLoginLog> findByRetailerIdAndVendorId(String retailerId, String vendorId);
}
