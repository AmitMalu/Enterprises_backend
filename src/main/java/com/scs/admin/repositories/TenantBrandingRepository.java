package com.scs.admin.repositories;

import com.scs.admin.entities.TenantBranding;
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

    Optional<TenantBranding> findByWhiteLabelId(String whiteLabelId);

}
