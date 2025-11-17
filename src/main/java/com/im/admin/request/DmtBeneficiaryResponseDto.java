package com.im.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryResponseDto {

    private Long id;
    private Long beneId;
    private String name;
    private String bankId;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private String verified;
    private String status;
}
