package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Hierarchy ID is required")
    @Column(name = "hierarchy_id", nullable = false)
    private String hierarchyId;

    @NotBlank(message = "Sub-product code is required")
    @Column(name = "sub_product_code", nullable = false)
    private String subProductCode;

    @NotBlank(message = "Product code is required")
    @Column(name = "product_code", nullable = false)
    private String productCode;

    @NotBlank(message = "Scheme code is required")
    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

}

