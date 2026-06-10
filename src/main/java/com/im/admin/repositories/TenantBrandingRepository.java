package com.im.admin.repositories;

import com.im.admin.entities.TenantBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * author abdul.M
 */
public interface TenantBrandingRepository  extends JpaRepository<TenantBranding, Long> {

    Optional<TenantBranding> findByDomainNameAndIsActiveTrue(String domainName);

    boolean existsByTenantCode(String tenantCode);

    boolean existsByDomainName(String domainName);

    Optional<TenantBranding> findByTenantCode(String tenantCode);

}
