package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;

public interface PayoutVendorService {

    String getVendorName();

    PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    //BeneficiaryVerifyResponseDto verifyBeneficiary(BeneficiaryVerifyRequestDto request, VendorCredentials credentials);

}

