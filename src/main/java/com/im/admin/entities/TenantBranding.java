package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_branding")
@Data
public class TenantBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "welcome_text")
    private String welcomeText;

    @Column(name = "is_active")
    private Boolean isActive;

}
