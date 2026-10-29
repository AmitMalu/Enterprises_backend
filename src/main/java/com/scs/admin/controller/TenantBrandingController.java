package com.scs.admin.controller;

import com.scs.admin.entities.TenantBranding;
import com.scs.admin.request.TenantBrandingRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.TenantConfigResponse;
import com.scs.admin.service.TenantBrandingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

        String origin = request.getHeader("X-Tenant-Domain");

        System.out.println("Origin = " + origin);

        TenantConfigResponse response =
                tenantBrandingService.getTenantConfig(origin);

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "Tenant configuration fetched successfully"
                )
        );
    }



    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CommonResponse<?> createTenant(

            @ModelAttribute TenantBrandingRequest request
    ) {

        return tenantBrandingService.createTenant(
                request
        );
    }


    @PostMapping(value = "/upload-logo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadLogo(
            @RequestParam("wlId") Long wlId,
            @RequestParam("image") MultipartFile image
    ) {

        return ResponseEntity.ok(
                tenantBrandingService.uploadLogo(wlId, image)
        );
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<TenantBranding>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TenantBranding> tenants = tenantBrandingService.findAll(pageable);
        return ResponseEntity.ok(
                CommonResponse.success(tenants, "Tenant branding fetched successfully")
        );
    }

    @GetMapping("/{whiteLabelId}")
    public ResponseEntity<CommonResponse<TenantBranding>> getById(
            @PathVariable String whiteLabelId
    ) {
        TenantBranding tenant = tenantBrandingService.findById(whiteLabelId);
        return ResponseEntity.ok(
                CommonResponse.success(tenant, "Tenant branding fetched successfully")
        );
    }
}
