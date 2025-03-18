package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scheme_allocations")
@Getter
@Setter
public class SchemeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hierarchy_id", nullable = false)
    private String hierarchyId;  // Retailer, Distributor, Partner, etc.

    @Column(name = "sub_product_code", nullable = false)
    private String subProductCode;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;
}

