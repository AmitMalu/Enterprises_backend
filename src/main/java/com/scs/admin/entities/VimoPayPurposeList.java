package com.scs.admin.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scs.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@Entity
@Table(name = "vimo_pay_purpose_list")
public class VimoPayPurposeList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @NotNull(message = "Vendor is required")
    private Vendors vendor;

    @Column(name = "purpose_name", nullable = false)
    private String purposeName;

    @Column(name = "purpose_code", nullable = false)
    private String purposeCode;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
