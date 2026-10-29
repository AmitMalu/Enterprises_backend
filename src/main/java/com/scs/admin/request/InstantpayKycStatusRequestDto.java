package com.scs.admin.request;

import lombok.Data;

@Data
public class InstantpayKycStatusRequestDto {
    private String retailerId;
    private String subProductId;
}
