package com.im.admin.dto;

import lombok.Data;

@Data
public class BankRequestDto {

    private String bankCode;
    private String bankCodeBi;
    private String bankName;
    private String ifscCode;
    private String imps;
    private String neft;
    private String rtgs;
    private String activeStatus;
    private Integer maxAccountNoLength;
    private String verification;
    private String logo; // Base64 or URL or file handling later
    private String createdBy;
}
