package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.request.AepsMiniStatementRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.AepsMiniStatementResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public class InstantPayAepsServiceImpl implements AepsVendorService{
    @Override
    public String getVendorCode() {
        return "";
    }

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(AepsCashWithdrawalRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials) {
        return null;
    }
}
