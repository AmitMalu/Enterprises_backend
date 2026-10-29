package com.scs.admin.repositories;

import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.DistributorPrepaidAmount;
import com.scs.admin.entities.RetailerPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorPrepaidAmountRepository extends JpaRepository<DistributorPrepaidAmount, Long> {

    void deleteByDistributorId(Distributor distributorId);

    Optional<DistributorPrepaidAmount> findByDistributorId_DistributorId(String distributorId);

    @Query("""
    SELECT COALESCE(SUM(d.availableBalance), 0)
    FROM DistributorPrepaidAmount d
    WHERE d.distributorId.distributorId IN :ids
""")
    BigDecimal sumAvailableBalanceByDistributors(@Param("ids") List<String> ids);



}
