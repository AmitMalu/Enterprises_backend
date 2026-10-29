package com.scs.admin.repositories;

import aj.org.objectweb.asm.commons.Remapper;
import com.scs.admin.entities.Enterprises;
import com.scs.admin.entities.EnterprisesPostpaidAmount;
import com.scs.admin.entities.EnterprisesPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnterprisesPrepaidAmountRepository extends JpaRepository<EnterprisesPrepaidAmount, Long> {

    void deleteByEnterprise(Enterprises enterprise);

    Optional<EnterprisesPrepaidAmount> findByEnterprise_EnterpriseId(String enterprisesId);

    @Query("""
    SELECT COALESCE(SUM(e.availableBalance), 0)
    FROM EnterprisesPrepaidAmount e
    WHERE e.enterprise.id IN :ids
""")
    BigDecimal sumAvailableBalanceByEnterprises(@Param("ids") List<String> ids);

}
