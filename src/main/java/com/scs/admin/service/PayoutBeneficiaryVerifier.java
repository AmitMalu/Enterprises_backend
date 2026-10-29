package com.scs.admin.service;

import com.scs.admin.dto.BeneficiaryVerifyRequestDto;
import com.scs.admin.dto.BeneficiaryVerifyResponseDto;
import com.scs.admin.entities.VendorCredentials;

public interface PayoutBeneficiaryVerifier {

    BeneficiaryVerifyResponseDto verifyBeneficiary(
            BeneficiaryVerifyRequestDto request,
            VendorCredentials credentials
    );
}
