package com.im.admin.service.impl;

import com.im.admin.dto.DmtResponseDto;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.entities.DmtBankList;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.service.DmtVendorService;

import java.util.List;

public class FingpayDmtServiceImpl implements DmtVendorService {
    @Override
    public List<DmtBankList> getdmtbanklist() {
        return List.of();
    }

    @Override
    public String generateJwtToken() {
        return "";
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl) {
        return null;
    }

    @Override
    public String getVendorCode() {
        return "Fingpay Dmt";
    }

    @Override
    public RemitterTransactionResponse processDmt(SendDmtRequest request, VendorCredentials credentials, String orderId, SubProducts subProducts, WalletType walletType) {
        return null;
    }

    @Override
    public PaysprintSendOtpResponse sendOtp(SendDmtRequest request, VendorCredentials credentials) {
        return null;
    }

}
