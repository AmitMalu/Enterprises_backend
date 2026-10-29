package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_branding")
@Data
public class TenantBranding {

    @Id
    @Column(name = "white_label_id")
    private String whiteLabelId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_label_id")
    private WhiteLabels whiteLabel;

    @Column(name = "tenant_code", unique = true)
    private String tenantCode;

    @Column(name = "domain_name", unique = true)
    private String domainName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "background_path")
    private String backgroundPath;

    @Column(name = "terms_path")
    private String termsPath;

    @Column(name = "privacy_path")
    private String privacyPath;

    @Column(name = "refund_path")
    private String refundPath;

    @Column(name = "favicon")
    private String favicon;

    @Column(name = "portal_title")
    private String portalTitle;

    @Column(name = "welcome_text")
    private String welcomeText;

    @Column(name = "login_artwork")
    private String loginArtwork;

    @Column(name = "is_active")
    private Boolean isActive;


}
