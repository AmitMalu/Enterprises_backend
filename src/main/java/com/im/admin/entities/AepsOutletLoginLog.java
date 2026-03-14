package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "aeps_")
public class AepsOutletLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String retailerId;
    private String vendorId;
    private String outletId;
    private String externalRef;
    private String statuscode;
    private String actcode;
    private String message;
    private LocalDateTime createdAt;
}
