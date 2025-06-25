package com.im.admin.entities;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;


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
    private Boolean imps;     //b

    @Column(name = "neft")
    private Boolean neft;     //b

    @Column(name = "rtgs")
    private Boolean rtgs;          //boolean

    @Column(name = "active_status")
    private Boolean isActive;     //boolean

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;  // changed from Date to LocalDateTime

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

}
