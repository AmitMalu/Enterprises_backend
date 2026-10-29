package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Entity
@Table(name = "white_label_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WhiteLabelUser {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount; // BZT-WL-001

    @Column(name = "abbreviation", length = 3)
    private String abbreviation;

    private String name;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    private String username;

    private String password;

    private String emailId;

    private Boolean emailVerified;

    private LocalDate dob;

    @Embedded
    private Address add;

    @Column(name = "status")
    private Boolean status = true;

    @CreationTimestamp
    private LocalDateTime createdOn;

    private String createdBy;

    @UpdateTimestamp
    private LocalDateTime lastEditedOn;

    private String editedBy;

    private String createdByIPAddress;

    private String permission;
}
