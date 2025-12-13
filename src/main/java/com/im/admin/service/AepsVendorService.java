package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsVendorService {

    String getVendorCode();

    CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(
            AepsCashWithdrawalRequest req,
            VendorCredentials credentials
    );
    ;

    ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials);
}
