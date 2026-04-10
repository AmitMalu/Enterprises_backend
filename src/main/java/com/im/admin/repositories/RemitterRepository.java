package com.im.admin.repositories;

import com.im.admin.entities.DmtRemitter;
import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemitterRepository extends JpaRepository<DmtRemitter, Long> {
    Optional<DmtRemitter> findByMobile(String mobile);
    Optional<DmtRemitter> findByMobileAndVendor(String mobile, Vendors vendor);
}

