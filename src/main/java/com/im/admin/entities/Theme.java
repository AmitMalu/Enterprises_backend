package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Table(name = "themes")
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "theme")
    private String theme;

    @Column(name = "base_color")
    private String baseColor;

    @Column(name = "logo")
    private String logo;

    @Column(name = "watermark_URL")
    private String watermarkURL;

    @Column(name = "login_Banner")
    private String loginBanner;

}