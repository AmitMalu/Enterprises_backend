package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "scs_logs")
public class ScsLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Integer srNo;

    @Lob
    @Column(name = "request", columnDefinition = "TEXT")
    private String request;

    @Lob
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "request_date_time")
    private LocalDateTime requestDateTime;

    @Column(name = "response_date_time")
    private LocalDateTime responseDateTime;
}
