package com.im.admin.repositories;

import com.im.admin.entities.VendorLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorReqResRepository extends JpaRepository<VendorLogs,Integer> {
}
