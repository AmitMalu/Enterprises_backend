package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sms_credentials_master")
public class SmsCredentialsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "pe_id")
    private String peId;

    @Column(name = "dlt_template_id")
    private String dltTemplateId;

    @Column(name = "route")
    private String route;

    @Column(name = "admin_id")
    private String adminId;

    @Column(name = "whitelabel_id")
    private String whitelabelId;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "template")
    private String template;

    @Column(name = "type")
    private String type;


}


