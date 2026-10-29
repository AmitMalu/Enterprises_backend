package com.scs.admin.repositories;

import com.scs.admin.entities.DistributorPrepaidAmount;
import com.scs.admin.entities.Partner;
import com.scs.admin.entities.PartnerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerPrepaidAmountRepository extends JpaRepository<PartnerPrepaidAmount, Long> {

    void deleteByPartnerId(Partner partner);

    Optional<PartnerPrepaidAmount> findByPartnerId_PartnerId(String partnerId);

    @Query("""
    SELECT COALESCE(SUM(p.availableBalance), 0)
    FROM PartnerPrepaidAmount p
    WHERE p.partnerId.partnerId IN :ids
""")
    BigDecimal sumAvailableBalanceByPartners(@Param("ids") List<String> ids);



}
