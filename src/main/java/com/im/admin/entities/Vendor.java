package com.im.admin.entities;
import jakarta.persistence.*;
import java.util.Date;

import lombok.Data;

@Entity
@Table(name = "vendor")
@Data
public class Vendor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "sub_product")
    private String subProduct;

    @Column(name = "api_vendor_name")
    private String apiVendorName;

    @Column(name = "api_vendor_class")
    private String apiVendorClass;

    @Column(name = "priority_order")
    private int priorityOrder;

    @Column(name = "website")
    private String  website;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "modes_supported")
    private String modesSupported;

    @Column(name = "bank_type")
    private String bankType;

    @Column(name = "blocked")
    private Boolean blocked;

    @Column(name = "edited_on")
    private Date editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}
