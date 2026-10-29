package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hierarchy_master")
@AllArgsConstructor
@NoArgsConstructor
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