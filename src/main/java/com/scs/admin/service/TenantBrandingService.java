package com.scs.admin.service;

import com.scs.admin.entities.TenantBranding;
import com.scs.admin.request.TenantBrandingRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.TenantConfigResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * author abdul.M
 */
public interface TenantBrandingService {

    TenantConfigResponse getTenantConfig(String hostName);

    CommonResponse<?> createTenant(
            TenantBrandingRequest request);

    String uploadLogo(Long wlId, MultipartFile image);

    Page<TenantBranding> findAll(Pageable pageable);

    TenantBranding findById(String whiteLabelId);
}
