package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "billpay_config")
public class BillPayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String vendor;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "provider_name", nullable = false, length = 200)
    private String providerName;

    @Lob
    @Column(name = "raw_config", columnDefinition = "JSON")
    private String rawConfig;    // Full config in JSON string

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Auto-set timestamps
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
