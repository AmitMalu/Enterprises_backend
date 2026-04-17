package com.im.admin.controller;


import com.im.admin.request.AepsBalanceInquiryRequest;
import com.im.admin.request.AepsMiniStatementRequest;
import com.im.admin.response.AepsBalanceInquiryResponse;
import com.im.admin.response.AepsMiniStatementResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsBalanceInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aeps")
@RequiredArgsConstructor
public class AepsBalanceInquiryController {

    @Autowired
    private AepsBalanceInquiryService aepsBalanceInquiryService;

    @PostMapping("/balance-inquiry")
    public CommonResponse<AepsBalanceInquiryResponse> balanceInquiry(
            @RequestBody AepsBalanceInquiryRequest req,
            @RequestHeader Double latitude, @RequestHeader Double longitude) {
        return aepsBalanceInquiryService.balanceInquiry(req,latitude,longitude);
    }

    @PostMapping("/mini-statement")
    public CommonResponse<AepsMiniStatementResponse> MiniStatemnet(
            @RequestBody AepsMiniStatementRequest req,
            @RequestHeader Double latitude, @RequestHeader Double longitude) {
        return aepsBalanceInquiryService.ministatement(req,latitude,longitude);
    }

}
