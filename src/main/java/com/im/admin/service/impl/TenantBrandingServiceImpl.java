package com.im.admin.service.impl;

import com.im.admin.entities.TenantBranding;
import com.im.admin.repositories.TenantBrandingRepository;
import com.im.admin.request.TenantBrandingRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.TenantConfigResponse;
import com.im.admin.service.TenantBrandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * author abdul.M
 */
@Service
public class TenantBrandingServiceImpl implements TenantBrandingService {

    @Autowired
    private TenantBrandingRepository repository;

    @Override
    public TenantConfigResponse getTenantConfig(String hostName) {

        TenantBranding tenant = repository
                .findByDomainNameAndIsActiveTrue(hostName)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No active tenant found for domain : "
                                        + hostName));

        return TenantConfigResponse.builder()
                .tenantCode(tenant.getTenantCode())
                .companyName(tenant.getCompanyName())
                .logoUrl(tenant.getLogoPath())
                .backgroundUrl(tenant.getBackgroundPath())
                .termsUrl(tenant.getTermsPath())
                .privacyUrl(tenant.getPrivacyPath())
                .refundUrl(tenant.getRefundPath())
                .primaryColor(tenant.getPrimaryColor())
                .secondaryColor(tenant.getSecondaryColor())
                .welcomeText(tenant.getWelcomeText())
                .build();
    }

    @Override
    public CommonResponse<?> createTenant(
            TenantBrandingRequest request) {

        if (repository.existsByTenantCode(request.getTenantCode())) {
            return CommonResponse.handleErrors(
                    "Tenant code already exists");
        }

        if (repository.existsByDomainName(request.getDomainName())) {
            return CommonResponse.handleErrors(
                    "Domain already exists");
        }

        TenantBranding tenant = new TenantBranding();

        tenant.setTenantCode(request.getTenantCode());
        tenant.setDomainName(request.getDomainName());
        tenant.setCompanyName(request.getCompanyName());

        tenant.setPrimaryColor(request.getPrimaryColor());
        tenant.setSecondaryColor(request.getSecondaryColor());

        tenant.setLogoPath(request.getLogoPath());
        tenant.setBackgroundPath(request.getBackgroundPath());

        tenant.setTermsPath(request.getTermsPath());
        tenant.setPrivacyPath(request.getPrivacyPath());
        tenant.setRefundPath(request.getRefundPath());

        tenant.setWelcomeText(request.getWelcomeText());

        tenant.setIsActive(
                request.getIsActive() == null
                        ? true
                        : request.getIsActive());

        repository.save(tenant);

        return CommonResponse.created(
                tenant,
                "Tenant created successfully");
    }

}
