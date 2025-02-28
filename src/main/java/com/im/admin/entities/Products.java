package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "operator_source")
    private String operatorSource;

    @Column(name = "cluster_mode")
    private Boolean clusterMode;

    @Column(name = "wallet_type", nullable = false)
    private String walletType;

    @Column(name = "is_inventory")
    private Boolean isInventory;

    @Column(name = "is_visible")
    private Boolean isVisible;

    @Column(name = "is_virtual")
    private Boolean isVirtual;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "reverse_tx")
    private Boolean reverseTx;

    @Column(name = "is_settlement")
    private Boolean isSettlement;

    @Column(name = "has_charges")
    private Boolean hasCharges;

    @Column(name = "is_commission")
    private Boolean isCommission;

    @Column(name = "dashboard_link")
    private String dashboardLink;

    @Column(name = "dashboard_link_for_apk")
    private String dashboardLinkForApk;

    @Column(name = "dashboard_image")
    private String dashboardImage;

    @Column(name = "dashboard_priority")
    private Integer dashboardPriority;

    @Column(name = "blocked")
    private Boolean blocked;

    @Column(name = "created_on")
    @Temporal(TemporalType.DATE)
    private Date createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "edited_on")
    @Temporal(TemporalType.DATE)
    private Date editedOn;

    @Column(name = "edited_by")
    private String editedBy;


    //@ManyToMany(mappedBy = "products") // Bidirectional mapping (optional)
    //private Set<WhiteLabels> whiteLabels;
}
