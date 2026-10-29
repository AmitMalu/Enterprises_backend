package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.entities.SubProducts;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.request.PayoutResponseDto;
import com.scs.admin.request.SendPayoutRequest;

import java.util.List;

public interface PayoutVendorService {

    List<String> getVendorName();

    PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    //BeneficiaryVerifyResponseDto verifyBeneficiary(BeneficiaryVerifyRequestDto request, VendorCredentials credentials);

}

