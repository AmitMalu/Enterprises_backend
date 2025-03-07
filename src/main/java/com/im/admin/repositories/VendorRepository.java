package com.im.admin.repositories;

import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendors, String> {

  @Query("SELECT MAX(v.id) FROM Vendors v")
  String findMaxId();  // Find the highest existing ID
}
