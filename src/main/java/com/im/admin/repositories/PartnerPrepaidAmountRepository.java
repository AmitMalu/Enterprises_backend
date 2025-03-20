package com.im.admin.repositories;

import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerPrepaidAmountRepository extends JpaRepository<PartnerPrepaidAmount, Long> {

    void deleteByPartnerId(Partner partner);


}
