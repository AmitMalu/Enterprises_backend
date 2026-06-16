package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NetworkBankMasterResponseDto {
    private Long id;
    private String bankName;
    private String ifscCode;
}
