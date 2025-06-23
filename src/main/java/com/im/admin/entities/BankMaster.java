package com.im.admin.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Data
@Table(name = "bank_master")
public class BankMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "imps")
    private String imps;

    @Column(name = "neft")
    private String neft;

    @Column(name = "rtgs")
    private String rtgs;

    @Column(name = "active_status")
    private String activeStatus;

    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "edited_on")
    private LocalDateTime editedOn;  // changed from Date to LocalDateTime

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

}
