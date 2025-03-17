package com.im.admin.repositories;

import com.im.admin.entities.VendorRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRoutingRepository extends JpaRepository<VendorRouting, Long> {


}
