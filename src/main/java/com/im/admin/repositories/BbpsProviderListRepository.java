package com.im.admin.repositories;

import com.im.admin.entities.BbpsProviderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BbpsProviderListRepository extends JpaRepository<BbpsProviderList, Long> {

    Optional<BbpsProviderList> findByVendorId(Integer vendorId);

}
