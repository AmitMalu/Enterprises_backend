package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@IdClass(ImSubMasterId.class)
@Table(name = "im_sub_master")
public class ImSubMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "im_sub_master_id")
    private Integer imSubMasterId;

    @Id
    @Column(name = "im_master_id")
    private String imMasterId;

    @Column(name = "im_sub_master_name")
    private String imSubMasterName;

    @Column(name = "im_sub_master_desc")
    private String imSubMasterDesc;

}
