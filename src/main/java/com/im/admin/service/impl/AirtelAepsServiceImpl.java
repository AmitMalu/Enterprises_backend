package com.im.admin.service.impl;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.service.AepsVendorService;
import org.springframework.http.ResponseEntity;

public class AirtelAepsServiceImpl implements AepsVendorService {
    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsCashWithdrawalResponse> processCashWithdrawal(AepsCashWithdrawalRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public String getVendorCode() {
        return "Airtel Aeps";
    }
}
