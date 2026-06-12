package com.im.admin.service.impl;

import com.im.admin.entities.TenantBranding;
import com.im.admin.repositories.TenantBrandingRepository;
import com.im.admin.request.TenantBrandingRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.TenantConfigResponse;
import com.im.admin.service.TenantBrandingService;
import com.im.admin.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * author abdul.M
 */
@Slf4j
@Service
public class TenantBrandingServiceImpl implements TenantBrandingService {


    @Autowired
    private TenantBrandingRepository repository;

    @Autowired
    private ImageUtil imageUtil;

    @Value("${file.access.config-assets-path}")
    private String assetBasePath;

    private static final String WL_ASSET_PATH =
            "/mnt/wl-assets";

//    @Override
//    public TenantConfigResponse getTenantConfig(String hostName) {
//
//        TenantBranding tenant = repository
//                .findByDomainNameAndIsActiveTrue(hostName)
//                .orElseThrow(() ->
//                        new RuntimeException(
//                                "No active tenant found for domain : "
//                                        + hostName));
//
//        return TenantConfigResponse.builder()
//                .tenantCode(tenant.getTenantCode())
//                .companyName(tenant.getCompanyName())
//                .logoUrl(tenant.getLogoPath())
//                .backgroundUrl(tenant.getBackgroundPath())
//                .termsUrl(tenant.getTermsPath())
//                .privacyUrl(tenant.getPrivacyPath())
//                .refundUrl(tenant.getRefundPath())
//                .primaryColor(tenant.getPrimaryColor())
//                .secondaryColor(tenant.getSecondaryColor())
//                .welcomeText(tenant.getWelcomeText())
//                .favicon(tenant.getFavicon())
//                .portalTitle(tenant.getPortalTitle())
//                .build();
//    }

    @Override
    public TenantConfigResponse getTenantConfig(String hostName) {

        TenantBranding tenant = repository
                .findByDomainNameAndIsActiveTrue(hostName)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No active tenant found for domain : "
                                        + hostName));

        String tenantCode = tenant.getTenantCode();

        return TenantConfigResponse.builder()
                .tenantCode(tenantCode)
                .companyName(tenant.getCompanyName())

                .logoUrl(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getLogoPath()))

                .backgroundUrl(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getBackgroundPath()))

                .termsUrl(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getTermsPath()))

                .privacyUrl(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getPrivacyPath()))

                .refundUrl(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getRefundPath()))

                .favicon(buildAssetUrl(
                        hostName,
                        tenantCode,
                        tenant.getFavicon()))

                .primaryColor(tenant.getPrimaryColor())
                .secondaryColor(tenant.getSecondaryColor())
                .welcomeText(tenant.getWelcomeText())
                .portalTitle(tenant.getPortalTitle())
                .build();
    }

    private String buildAssetUrl(String domain,
                                 String tenantCode,
                                 String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        return "https://"
                + domain
                + assetBasePath
                + "/"
                + tenantCode
                + "/"
                + fileName;
    }

    @Override
    public CommonResponse<?> createTenant(
            TenantBrandingRequest request) {

        try {

            TenantBranding tenant =
                    repository.findByTenantCode(
                                    request.getTenantCode())
                            .orElse(null);

            boolean isNewTenant = tenant == null;

            if (isNewTenant) {

                if (repository.existsByDomainName(
                        request.getDomainName())) {

                    return CommonResponse.handleErrors(
                            "Domain already exists");
                }

                tenant = new TenantBranding();

                tenant.setTenantCode(
                        request.getTenantCode());

                tenant.setWhiteLabelId(
                        request.getWhiteLabelId());
            }

            tenant.setDomainName(
                    request.getDomainName());

            tenant.setCompanyName(
                    request.getCompanyName());

            tenant.setPrimaryColor(
                    request.getPrimaryColor());

            tenant.setSecondaryColor(
                    request.getSecondaryColor());

            tenant.setPortalTitle(
                    request.getPortalTitle());

            tenant.setWelcomeText(
                    request.getWelcomeText());

            tenant.setIsActive(
                    request.getIsActive() == null
                            ? true
                            : request.getIsActive());

            String tenantCode =
                    tenant.getTenantCode();

            if (request.getLogo() != null
                    && !request.getLogo().isEmpty()) {

                tenant.setLogoPath(
                        saveFile(
                                tenantCode,
                                request.getLogo(),
                                "logo"));
            }

            if (request.getBackground() != null
                    && !request.getBackground().isEmpty()) {

                tenant.setBackgroundPath(
                        saveFile(
                                tenantCode,
                                request.getBackground(),
                                "loginBackground"));
            }

            if (request.getFaviconFile() != null
                    && !request.getFaviconFile().isEmpty()) {

                tenant.setFavicon(
                        saveFile(
                                tenantCode,
                                request.getFaviconFile(),
                                "favicon"));
            }

            if (request.getTerms() != null
                    && !request.getTerms().isEmpty()) {

                tenant.setTermsPath(
                        saveFile(
                                tenantCode,
                                request.getTerms(),
                                "terms"));
            }

            if (request.getPrivacy() != null
                    && !request.getPrivacy().isEmpty()) {

                tenant.setPrivacyPath(
                        saveFile(
                                tenantCode,
                                request.getPrivacy(),
                                "privacy"));
            }

            if (request.getRefund() != null
                    && !request.getRefund().isEmpty()) {

                tenant.setRefundPath(
                        saveFile(
                                tenantCode,
                                request.getRefund(),
                                "refund"));
            }

            repository.save(tenant);

            log.info(
                    "Tenant saved successfully. tenantCode={}",
                    tenantCode);

            return CommonResponse.success(
                    tenant,
                    isNewTenant
                            ? "Tenant created successfully"
                            : "Tenant updated successfully");

        } catch (Exception e) {

            log.error(
                    "Tenant save failed. tenantCode={}, error={}",
                    request.getTenantCode(),
                    e.getMessage(),
                    e);

            return CommonResponse.handleErrors(
                    e.getMessage());
        }
    }

    @Override
    public String uploadLogo(Long wlId, MultipartFile image) {

        log.info("Logo upload request received. wlId={}, fileName={}",
                wlId, image.getOriginalFilename());

        try {

            TenantBranding branding = repository.findById(wlId)
                    .orElseThrow(() -> {
                        log.error("Tenant branding not found for wlId={}", wlId);
                        return new RuntimeException("White label not found");
                    });

            String tenantCode = branding.getTenantCode();

            log.info("Tenant found. tenantCode={}, domainName={}",
                    tenantCode, branding.getDomainName());

            Path tenantFolder = Paths.get(WL_ASSET_PATH, tenantCode);

            log.info("Creating tenant folder if not exists: {}",
                    tenantFolder);

            Files.createDirectories(tenantFolder);

            Files.setPosixFilePermissions(
                    tenantFolder,
                    PosixFilePermissions.fromString("rwxr-xr-x")
            );

            String fileName =
                    "logo_" + System.currentTimeMillis()
                            + "_" + image.getOriginalFilename();

            Path filePath = tenantFolder.resolve(fileName);

            log.info("Saving file to path={}", filePath);

            image.transferTo(filePath.toFile());

            Files.setPosixFilePermissions(
                    filePath,
                    PosixFilePermissions.fromString("rw-r--r--")
            );

            log.info("File saved successfully. path={}, size={} bytes",
                    filePath,
                    image.getSize());

            branding.setLogoPath(
                    tenantCode + "/" + fileName
            );

            repository.save(branding);

            log.info("Database updated successfully. wlId={}, logoPath={}",
                    wlId,
                    branding.getLogoPath());

            return "Uploaded Successfully : " + filePath;

        } catch (Exception e) {

            log.error("Logo upload failed for wlId={}. Error={}",
                    wlId,
                    e.getMessage(),
                    e);

            throw new RuntimeException(
                    "Upload Failed : " + e.getMessage()
            );
        }
    }

    private String saveFile(
            String tenantCode,
            MultipartFile file,
            String prefix
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        Path tenantFolder =
                Paths.get(
                        WL_ASSET_PATH,
                        tenantCode
                );

        Files.createDirectories(
                tenantFolder);

        Files.setPosixFilePermissions(
                tenantFolder,
                PosixFilePermissions.fromString("rwxr-xr-x")
        );

        String originalFileName =
                file.getOriginalFilename();

        String extension = "";

        if (originalFileName != null
                && originalFileName.contains(".")) {

            extension =
                    originalFileName.substring(
                            originalFileName.lastIndexOf("."));
        }

        String fileName =
                prefix + extension;

        Path filePath =
                tenantFolder.resolve(
                        fileName);

        file.transferTo(
                filePath.toFile());

        Files.setPosixFilePermissions(
                filePath,
                PosixFilePermissions.fromString("rw-r--r--")
        );

        return tenantCode
                + "/"
                + fileName;
    }




}
