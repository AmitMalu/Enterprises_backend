package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

/**
 * author abdul.M
 */
@Data
@Builder
public class TenantConfigResponse {

    private String tenantCode;
    private String companyName;
    private String logoUrl;
    private String backgroundUrl;
    private String termsUrl;
    private String privacyUrl;
    private String refundUrl;
    private String primaryColor;
    private String secondaryColor;
    private String welcomeText;
    private String favicon;
    private String portalTitle;

}
