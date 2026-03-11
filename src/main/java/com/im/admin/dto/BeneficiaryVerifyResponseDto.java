package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryVerifyResponseDto {

    private String beneficiaryName;

    private String verifyId;

    private String status;

    private String message;
}
