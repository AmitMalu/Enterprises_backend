package com.scs.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@IdClass(UserDetailsId.class)
@Entity
@Table(name = "user_details")
public class UserDetails {

    @Id
    @Column(name = "mobile_number", nullable = false)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @Id
    @Column(name = "hierarchy_id")
    private String hierarchyId;  //ABC-RT-578, YUR-ET-674, FHR-PT-675

    @Column(name = "hierarchy_code")
    private String hierarchyCode;  //RT, DT, PT

    @Column(name = "login_username", nullable = false)
    private String loginUserName;

    @Column(name = "login_passwd")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/~`]).{8,}$",
            message = "Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, and one special character"
    )
    private String loginPassword;

    @Column(name = "user_login_status")
    private Boolean userLoginStatus = false;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Column(name = "last_login_time")
    private LocalTime lastLoginTime;

    @Column(name = "user_status")
    private String userStatus = "Pending";

    @Column(name = "otp")
    private String otp;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String emailId;

    @Column(name = "otp_expiry_time")
    private LocalTime otpExpiryTime;

    @Column(name = "channel")
    private String channel;   //web or mobile

    @Column(name = "geo_location")
    private String geoLocation;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalTime createdTime;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "edited_date", nullable = false, updatable = true)
    private LocalDate editedDate;

    @CreationTimestamp
    @Column(name = "edited_time", nullable = false, updatable = true)
    private LocalTime editedTime;

    @Column(name = "edited_by", nullable = false, updatable = true)
    private String editedBy;

    @Column(name = " role_id")
    private String  role_id;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

    @Column(name = "permission")
    private String permission;

    @Column(name = "parent_account")
    private String parentAccount;


}
