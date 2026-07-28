package com.im.admin.service;

import com.im.admin.entities.TenantBranding;
import com.im.admin.request.TenantBrandingRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.TenantConfigResponse;
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
