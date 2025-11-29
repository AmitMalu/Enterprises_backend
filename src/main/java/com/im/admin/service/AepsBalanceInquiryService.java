package com.im.admin.service;

import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import org.springframework.http.ResponseEntity;

public interface AepsBalanceInquiryService {
    ResponseEntity<AepsBalanceInquiryResponse> balanceInquiry(AepsBalanceInquiryRequest req);
}
