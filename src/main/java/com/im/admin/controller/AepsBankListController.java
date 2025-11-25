package com.im.admin.controller;


import com.im.admin.response.FingpayBankApiResponse;
import com.im.admin.service.AepsBankListService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aeps")
public class AepsBankListController {

@Autowired
private AepsBankListService aepsBankListService;

@GetMapping("/bank-list")
    public FingpayBankApiResponse getFullVendorBankList() {
        return aepsBankListService.getFullBankList();
    }
}
