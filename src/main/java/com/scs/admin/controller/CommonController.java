package com.scs.admin.controller;

import com.scs.admin.response.WalletBalanceResponseDto;
import com.scs.admin.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common")
@Slf4j
public class CommonController {

    @Autowired
    private CommonService commonService;

    @GetMapping("/balance/{hierarchyId}")
    public ResponseEntity<WalletBalanceResponseDto> fetchWalletBalance(@PathVariable("hierarchyId") String hierarchyId) {
        log.info("API Call: Fetch Wallet Balance for hierarchyId: {}", hierarchyId);
        WalletBalanceResponseDto response = commonService.fetchWalletBalance(hierarchyId);
        return ResponseEntity.ok(response);
    }



}
