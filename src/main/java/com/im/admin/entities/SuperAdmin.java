package com.im.admin.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "super_admin")
public class SuperAdmin extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Mobile Number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @NotBlank(message = "Email is mandatory")
    @Column(name = "email_id", nullable = false, unique = true)
    private String emailId;

    @NotBlank(message = "User Name is mandatory")
    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    @NotBlank(message = "Password is mandatory")
    @Column(name = "password", nullable = false)
    private String password;


}


