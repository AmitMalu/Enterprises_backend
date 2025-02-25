package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Mobile Number is mandatory")
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @NotBlank(message = "Email is mandatory")
    @Column(name = "email_id", nullable = false)
    private String emailId;

    @NotBlank(message = "User Name is mandatory")
    @Column(name = "user_name", nullable = false)
    private String userName;

    @NotBlank(message = "Password is mandatory")
    @Column(name = "password", nullable = false)
    private String password;


}