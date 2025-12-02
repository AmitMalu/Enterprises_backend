package com.im.admin.controller;


import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsBalanceInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aeps")
@RequiredArgsConstructor
public class AepsBalanceInquiryController {

    @Autowired
    private AepsBalanceInquiryService aepsBalanceInquiryService;

    @PostMapping("/balance-inquiry")
    public CommonResponse<AepsBalanceInquiryResponse> balanceInquiry(
            @RequestBody AepsBalanceInquiryRequest req) {
        return aepsBalanceInquiryService.balanceInquiry(req);
    }

}
