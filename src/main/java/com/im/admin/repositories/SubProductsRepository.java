package com.im.admin.repositories;

import com.im.admin.entities.SubProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubProductsRepository extends JpaRepository<SubProducts, String>, JpaSpecificationExecutor<SubProducts> {

    Optional<SubProducts> findBySubProductCodeOrSubProductTitle(String subProductCode, String subProductTitle);

    List<SubProducts> findByProductIdAndIsActiveTrue(Short productId);

    Optional<SubProducts> findBySubProductCode(String subProductCode);

    @Query("SELECT sp.isSettlement FROM SubProducts sp WHERE sp.subProductCode = :subProductCode")
    Boolean findIsSettlementByCode(@Param("subProductCode") String subProductCode);

    Optional<SubProducts> findBySubProductTitle(String subProductTitle);

    List<SubProducts> findByIsActiveTrue();


}


