package com.im.admin.repositories;

import com.im.admin.entities.VimoPayPurposeList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VimoPayPurposeListRepository extends JpaRepository<VimoPayPurposeList, Long> {

    Optional<VimoPayPurposeList> findByVendor_IdAndPurposeName(
            String vendorId,
            String purposeName
    );
}