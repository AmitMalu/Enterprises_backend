package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scs_sub_master")
public class ImSubMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scs_sub_master_id")
    private Integer imSubMasterId;


    @Column(name = "scs_master_id")
    private Integer  imMasterId;

    @Column(name = "scs_sub_master_name")
    private String imSubMasterName;

    @Column(name = "scs_sub_master_desc")
    private String imSubMasterDesc;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;


}
