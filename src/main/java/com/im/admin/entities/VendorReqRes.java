package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_vendor_request_response_logs")
public class VendorReqRes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sr_no")
    private Integer srNo;
    @Column(name = "im_vendor_id")
    private String imVendorId;
    @Column(name = "request")
    private String request;
    @Column(name = "response")
    private String response;
    @Column(name = "request_ip")
    private String requestIp;
    @Column(name = "request_date_time")
    private LocalDateTime requestDateTime;
    @Column(name = "response _date_time")
    private LocalDateTime responseDateTime;


}
