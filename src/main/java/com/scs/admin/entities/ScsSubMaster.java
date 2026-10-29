package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scs_sub_master")
public class ScsSubMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scs_sub_master_id")
    private Integer scsSubMasterId;


    @Column(name = "scs_master_id")
    private Integer  scsMasterId;

    @Column(name = "scs_sub_master_name")
    private String scsSubMasterName;

    @Column(name = "scs_sub_master_desc")
    private String scsSubMasterDesc;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;


}
