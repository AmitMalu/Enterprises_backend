package com.im.admin.repositories;

import com.im.admin.entities.DistributorPrepaidAmount;
import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerPrepaidAmountRepository extends JpaRepository<PartnerPrepaidAmount, Long> {

    void deleteByPartnerId(Partner partner);

    Optional<PartnerPrepaidAmount> findByPartnerId_PartnerId(String partnerId);



}
