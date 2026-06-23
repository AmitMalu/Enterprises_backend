package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.im.admin.entities.vendor.Vendors;
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
@Table(name = "vimo_pay_state_list")
public class VimoPayStateList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @NotNull(message = "Vendor is required")
    private Vendors vendor;

    @Column(name = "state_name", nullable = false)
    private String stateName;

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
