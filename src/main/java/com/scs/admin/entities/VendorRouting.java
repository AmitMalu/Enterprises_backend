package com.scs.admin.entities;


import com.scs.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vendor_routing")
public class VendorRouting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_product_id", referencedColumnName = "id")
    private SubProducts subProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_priority_1", referencedColumnName = "id")
    private Vendors routingPriority1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_priority_2", referencedColumnName = "id")
    private Vendors routingPriority2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_priority_3", referencedColumnName = "id")
    private Vendors routingPriority3;

    @Column(name = "volume_1")
    private String volume1;

    @Column(name = "volume_2")
    private String volume2;

    @Column(name = "volume_3")
    private String volume3;

    @Column(name = "volume_number_of_txn")
    private Integer volumeNumberOfTxn;

    @Column(name = "volume_value_of_txn")
    private Integer volumeValueOfTxn;

    @Column(name = "value_1")
    private String value1;

    @Column(name = "value_2")
    private String value2;

    @Column(name = "value_3")
    private String value3;

    @Column(name = "value_value_number_of_txn")
    private Integer valueValueNumberOfTxn;

    @Column(name = "value_value_of_txn")
    private Integer valueValueOfTxn;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

}
