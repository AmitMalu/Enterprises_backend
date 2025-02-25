package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hierarchy_master")
public class HierarchyMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "sr_no")
    private Integer srNo;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

}