package com.im.admin.repositories;

import com.im.admin.entities.TenantBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * author abdul.M
 */
public interface TenantBrandingRepository  extends JpaRepository<TenantBranding, Long> {

    Optional<TenantBranding> findByDomainNameAndIsActiveTrue(String domainName);

    boolean existsByTenantCode(String tenantCode);

    boolean existsByDomainName(String domainName);

    Optional<TenantBranding> findByTenantCode(String tenantCode);

    @Query("SELECT tb.logoPath FROM TenantBranding tb " +
            "WHERE tb.whiteLabelId = :whiteLabelId")
    String findByWhiteLabelId(@Param("whiteLabelId") String whiteLabelId);

}
