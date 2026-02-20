package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NetworkBankMasterResponseDto {
    private String bankId;
    private String bankName;
    private String ifscCode;
}
