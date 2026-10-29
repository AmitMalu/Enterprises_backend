package com.scs.admin.controller;


import com.scs.admin.response.FingpayBankApiResponse;
import com.scs.admin.service.AepsBankListService;
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
