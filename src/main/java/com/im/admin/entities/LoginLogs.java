package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Login_logs")
public class
LoginLogs {

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
