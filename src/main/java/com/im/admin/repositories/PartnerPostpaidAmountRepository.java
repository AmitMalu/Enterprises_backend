package com.im.admin.repositories;


import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPostpaidAmount;
import com.im.admin.entities.PartnerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerPostpaidAmountRepository extends JpaRepository<PartnerPostpaidAmount, Long> {

    void deleteByPartnerId(Partner partner);

    Optional<PartnerPostpaidAmount> findByPartnerId_PartnerId(String partnerId);


}
