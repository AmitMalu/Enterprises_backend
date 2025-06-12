package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "im_master")
public class ImMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "im_master_id")
    private Integer imMasterId;

    @Column(name = "im_master_name")
    private String imMasterName;

    @Column(name = "description")
    private String description;

}
