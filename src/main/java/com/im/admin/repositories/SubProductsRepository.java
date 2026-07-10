package com.im.admin.repositories;

import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    List<SubProducts> findByProductIdAndIsActiveTrueAndIsSettlement(Short productId, Boolean isSettlement);

    Optional<SubProducts> findBySubProductCodeAndIsVisibleTrue(@NotBlank(message = "Sub-product code is required") String subProductCode);

    List<SubProducts> findByProduct(Products product);

    @Query("""
            
            select s from SubProducts s
            
            where s.isWhiteLabelAdmin=true and s.whiteLabelAdminId=:wlId
            
            """)
    List<SubProducts> findAllowedWLSubProducts(@Param("wlId") String wlId);

    @Query("""
            
            select s
            
            from SubProducts s
            
            where s.product.id=:productId and s.isActive=true
            
            and (:settlement is null or s.isSettlement=:settlement)
            
            and s.isWhiteLabelAdmin=true
            
            and s.whiteLabelAdminId=:wlId
            
            """)
    List<SubProducts>
    findWLSubProducts(@Param("productId") Short productId, @Param("settlement") Boolean settlement, @Param("wlId") String wlId);

    @Query("""
            
            select s from SubProducts s
            
            where s.isActive=true
            
            and s.isWhiteLabelAdmin=true
            
            and s.whiteLabelAdminId=:wlId
            
            """)
    List<SubProducts>
    findAssignedWLSubProducts(

            @Param("wlId")
            String wlId
    );


    List<SubProducts> findAllBySubProductCodeIn(
            Collection<String> subProductCodes
    );

}



