package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "admin")
@Getter
@Setter
public class Admin extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @NotBlank(message = "Name is mandatory")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Mobile Number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
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

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "rejection_reason")
    private String rejectionReason = "-";

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_by_ip_address", nullable = false, updatable = false)
    private String createdByIPAddress;

}
