package com.scs.admin.service;

import com.scs.admin.request.AepsBalanceInquiryRequest;
import com.scs.admin.request.AepsMiniStatementRequest;
import com.scs.admin.response.AepsBalanceInquiryResponse;
import com.scs.admin.response.AepsMiniStatementResponse;
import com.scs.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AepsBalanceInquiryService {
    CommonResponse<AepsBalanceInquiryResponse> balanceInquiry(AepsBalanceInquiryRequest req,  Double latitude,  Double longitude);

    CommonResponse<AepsMiniStatementResponse> ministatement(AepsMiniStatementRequest req,  Double latitude,  Double longitude);
}
