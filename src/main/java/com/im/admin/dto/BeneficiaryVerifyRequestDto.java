package com.im.admin.dto;

import lombok.Data;

@Data
public class BeneficiaryVerifyRequestDto {

    private String subProductId;

    private String customerMobile;

    private String customerName;

    private String accountNumber;

    private String ifsc;

    private String bankId;

}
