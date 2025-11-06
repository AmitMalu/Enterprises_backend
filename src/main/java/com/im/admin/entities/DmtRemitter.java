package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "remitter_master")
public class DmtRemitter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobile;
    private String aadhaar;

    private String ekycId;
    private String stateResp;

    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String status; // PENDING / REGISTERED

    private LocalDateTime createdAt = LocalDateTime.now();
}
