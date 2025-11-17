package com.im.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemitterUpdateRequest {

    private Long id;              // remitter primary key
    private String mobile;        // mobile number
    private String aadhaar;       // aadhaar number (nullable)
    private String ekycId;        // ekyc id
    private String stateResp;     // state response
    private Double limitAmount;   // limit amount
    private String name;          // name of remitter
    private String status;        // REGISTERED / PENDING / etc.
    private String createdAt;     // optional - not recommended to update, but included as per JSON
}
