package com.im.admin.controller;

import com.im.admin.response.WalletBalanceResponseDto;
import com.im.admin.service.CommonService;
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
        WalletBalanceResponseDto response = commonService.fetchWalletBalance(hierarchyId);
        return ResponseEntity.ok(response);
    }


}
