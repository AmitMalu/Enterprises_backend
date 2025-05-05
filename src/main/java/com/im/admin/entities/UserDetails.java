package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "user_details")
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "mobile_no")
    private String mobileNumber;

    @Column(name = "login_username", unique = true, nullable = false)
    private String loginUserName;

    @Column(name = "login_passwd")
    private String loginPassword;

    @Column(name = "user_login_status")
    private Boolean userLoginStatus = false;

    @Column(name = "user_status")
    private String userStatus = "Pending";

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generation_time")
    private LocalTime otpGenerationTime;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalTime createdTime;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "edited_date", nullable = false, updatable = false)
    private LocalDate editedDate;

    @CreationTimestamp
    @Column(name = "edited_time", nullable = false, updatable = false)
    private LocalTime editedTime;

    @Column(name = "edited_by", nullable = false, updatable = false)
    private String editedBy;


}
