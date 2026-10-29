package com.scs.admin.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scs.admin.entities.enums.RateType;
import com.scs.admin.entities.enums.SlabType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scheme_rate")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "operator_code")
    private String operatorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type")
    private RateType rateType; // FLAT or PERCENTAGE

    @Enumerated(EnumType.STRING)
    @Column(name = "slab_type", columnDefinition = "varchar(20) default 'VARIABLE'")
    private SlabType slabType = SlabType.VARIABLE; // FIXED or VARIABLE

    // For Variable slab type
    @Column(name = "slab_start")
    private Double slabStart;

    @Column(name = "slab_end")
    private Double slabEnd;

    // Rate values
    @Column(name = "gross")
    private Double gross;

    @Column(name = "scs")
    private Double scs;

    @Column(name = "enterprise")
    private Double enterprise;

    @Column(name = "whitelabel")
    private Double whitelabel;

    @Column(name = "super_distributor")
    private Double superDistributor;

    @Column(name = "partner")
    private Double partner;

    @Column(name = "distributor")
    private Double distributor;

    @Column(name = "retailer")
    private Double retailer;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

}
