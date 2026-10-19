package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scs_master")
public class ImMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scs_master_id")
    private Integer imMasterId;

    @Column(name = "scs_master_name")
    private String imMasterName;

    @Column(name = "description")
    private String description;


    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

}
