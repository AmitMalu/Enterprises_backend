package com.im.admin.service;

import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsMiniStatementRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsMiniStatementResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AepsBalanceInquiryService {
    CommonResponse<AepsBalanceInquiryResponse> balanceInquiry(AepsBalanceInquiryRequest req,  Double latitude,  Double longitude);

    CommonResponse<AepsMiniStatementResponse> ministatement(AepsMiniStatementRequest req,  Double latitude,  Double longitude);
}
