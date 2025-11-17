package com.im.admin.response;

import lombok.Data;

@Data
public class DmtRemitterResponseDto {

    private Long id;
    private String mobile;
    private String aadhaar;
    private String ekycId;
    private String stateResp;
    private Double limitAmount;
    private String name;
    private String status;
    private String createdAt;
}
