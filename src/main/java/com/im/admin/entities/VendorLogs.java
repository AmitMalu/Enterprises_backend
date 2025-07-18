package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_vendor_request_response_logs")
public class VendorLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Integer srNo;

    @Column(name = "im_vendor_id")
    private String imVendorId;

    @Lob
    @Column(name = "request", columnDefinition = "TEXT")
    private String request;

    @Lob
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "request_ip")
    private String requestIp;

    @Column(name = "request_date_time")
    private LocalDateTime requestDateTime;

    @Column(name = "response_date_time")
    private LocalDateTime responseDateTime;



}
