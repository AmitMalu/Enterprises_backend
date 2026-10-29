package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "scs_sub_role")
@Data
public class ScsSubRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sub_role_id")
    private  String subRoleId;

    @Column(name = "mobile_number")
    private String mobileNumber;

}
