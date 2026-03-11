package com.im.admin.service;

import com.im.admin.dto.BeneficiaryVerifyRequestDto;
import com.im.admin.dto.BeneficiaryVerifyResponseDto;
import com.im.admin.entities.VendorCredentials;

public interface PayoutBeneficiaryVerifier {

    BeneficiaryVerifyResponseDto verifyBeneficiary(
            BeneficiaryVerifyRequestDto request,
            VendorCredentials credentials
    );
}
