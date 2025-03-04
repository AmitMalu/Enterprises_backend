package com.im.admin.repositories;

import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendors, Long> {
  }