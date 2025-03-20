package com.im.admin.repositories;


import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerPostpaidAmountRepository extends JpaRepository<PartnerPostpaidAmount, Long> {

    void deleteByPartnerId(Partner partner);

}
