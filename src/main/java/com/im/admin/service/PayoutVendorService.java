package com.im.admin.service;

import com.im.admin.dto.BeneficiaryVerifyRequestDto;
import com.im.admin.dto.BeneficiaryVerifyResponseDto;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;

public interface PayoutVendorService {

    String getVendorCode();

    PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType);

    //BeneficiaryVerifyResponseDto verifyBeneficiary(BeneficiaryVerifyRequestDto request, VendorCredentials credentials);

}

