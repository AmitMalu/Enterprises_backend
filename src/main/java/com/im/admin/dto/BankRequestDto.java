package com.im.admin.dto;

import lombok.Data;

@Data
public class BankRequestDto {

    private String bankCode;
    private String bankCodeBi;
    private String bankName;
    private String ifscCode;
    private Boolean imps;
    private Boolean neft;
    private Boolean rtgs;
    private Boolean isActive;
    private Integer maxAccountNoLength;
    private String logo; // Base64 or URL or file handling later
    private String createdBy;
    private Boolean isBlocked;
    private String editedBy;
}
