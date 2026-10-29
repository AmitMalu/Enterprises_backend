package com.scs.admin.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * author abdul.M
 */

@Data
public class TenantBrandingRequest {

    private String whiteLabelId;

    private String tenantCode;

    private String domainName;

    private String companyName;

    private String primaryColor;

    private String secondaryColor;

    private MultipartFile logo;

    private MultipartFile background;

    private MultipartFile faviconFile;

    private MultipartFile terms;

    private MultipartFile privacy;

    private MultipartFile refund;

    private String portalTitle;

    private String welcomeText;

    private Boolean isActive;

    private MultipartFile loginArtwork;


}
