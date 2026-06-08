package com.im.admin.controller;

import com.im.admin.request.TenantBrandingRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.TenantConfigResponse;
import com.im.admin.service.TenantBrandingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * author abdul.M
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantBrandingController {

    @Autowired
    private TenantBrandingService tenantBrandingService;

    @GetMapping("/config")
    public ResponseEntity<CommonResponse<TenantConfigResponse>> getTenantConfig(
            HttpServletRequest request) {

        String hostName = request.getServerName();

        TenantConfigResponse response =
                tenantBrandingService.getTenantConfig(hostName);

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "Tenant configuration fetched successfully"
                )
        );
    }


    @PostMapping("/create")
    public CommonResponse<?> createTenant(
            @RequestBody TenantBrandingRequest request) {

        return tenantBrandingService.createTenant(request);
    }
}
