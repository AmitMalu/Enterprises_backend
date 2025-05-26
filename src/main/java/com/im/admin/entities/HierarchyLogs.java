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
@Table(name = "hierarchy_logs")
public class HierarchyLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Integer srNo;
    @Column(name = "request")
    private String request;
    @Column(name = "response")
    private String response;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "request_date_time")
    private LocalDateTime requestDateTime;
    @Column(name = "response_date_time")
    private LocalDateTime responseDateTime;

}
