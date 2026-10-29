package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scs_master")
public class ScsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scs_master_id")
    private Integer scsMasterId;

    @Column(name = "scs_master_name")
    private String scsMasterName;

    @Column(name = "description")
    private String description;


    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

}
