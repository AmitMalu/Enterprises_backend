package com.scs.admin.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification_master")
public class NotificationMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Info
    private String notificationName;
    private String internalCode;

    // Dropdowns
    private String alertType;
    private String product;

    // Content
    private String title;

    @Column(length = 500)
    private String shortMessage;

    @Column(length = 2000)
    private String longDescription;

    // Delivery
    private String deliveryChannel;   // WEB / APP / BOTH
    private String platform;          // ANDROID / WEB / BOTH

    // Targeting
    private String severity;          // LOW / MEDIUM / HIGH
    private String audienceType; // ALL / ROLE / USER
    private String role;         // for ROLE targeting
    private String hierarchyId;

    // CTA
    private String ctaType;
    private String ctaLabel;
    private String ctaAction;

    // Scheduling
    private String sendMode;          // NOW / SCHEDULE / EVENT
    private LocalDateTime scheduleTime;

    // Approval
    private Boolean requireApproval;
    private String approvalGroup;

    // Tracking
    private Boolean trackClick;

    private String active;

    // Audit
    private LocalDateTime createdAt = LocalDateTime.now();
    private String createdBy;

}
