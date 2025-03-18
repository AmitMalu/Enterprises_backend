package com.im.admin.repositories;

import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendors, String> {

  List<Vendors> findBySubProductIdAndActiveTrue(String subProductId);

  boolean existsByVendorCode(String vendorCode);


}
