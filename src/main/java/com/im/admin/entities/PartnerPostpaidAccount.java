package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;


import java.util.Date;

@Data
@Entity
@Table(name = "partner_postpaid_account")
public class PartnerPostpaidAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Long srNo;

    @Column(name = "name")
    private String name;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "im_merchant_number")
    private String imMerchantNumber;

    @Column(name = "wallet")
    private String wallet;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on", columnDefinition = "DATETIME")
    private Date createdOn;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_on", columnDefinition = "DATETIME")
    private Date updatedOn;

    @Column(name = "created_ip")
    private String createdIp;

}