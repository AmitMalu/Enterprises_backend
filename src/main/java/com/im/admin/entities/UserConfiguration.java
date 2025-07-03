package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@Table(name = "user_configuration")
public class UserConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "state")
    private String state;

    @Column(name = "user_role")
    private String userRole;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @Column(name = "permission")
    private String permission;
}
