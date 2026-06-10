package com.im.admin.controller;

import com.im.admin.request.TenantBrandingRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.TenantConfigResponse;
import com.im.admin.service.TenantBrandingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
}
