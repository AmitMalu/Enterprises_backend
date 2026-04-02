package com.im.admin.dto;

import lombok.Data;

@Data
public class DmtBeneficiaryDeleteResponse {
    private boolean status;
    private int response_code;
    private String message;
    private String referenceKey;
}
