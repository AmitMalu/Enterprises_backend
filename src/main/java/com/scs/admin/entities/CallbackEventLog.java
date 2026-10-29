package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "callback_event_log")
public class CallbackEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

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

    @Column(name = "order_id")
    private String orderId;

}
