package com.im.admin.request;

import lombok.Data;

/**
 * author abdul.M
 */

@Data
public class TenantBrandingRequest {

    private String tenantCode;

    private String domainName;

    private String companyName;

    private String primaryColor;

    private String secondaryColor;

    private String logoPath;

    private String backgroundPath;

    private String termsPath;

    private String privacyPath;

    private String refundPath;

    private String welcomeText;

    private Boolean isActive;

}
