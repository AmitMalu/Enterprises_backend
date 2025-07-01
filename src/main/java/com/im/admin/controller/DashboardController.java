package com.im.admin.controller;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @PostMapping("/get")
    public ResponseEntity<DashboardResponseDto> getAdminDashboard() {
        log.info("Inside getAdminDashboard controller method");
        DashboardResponseDto response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet-balance/{hierarchyId}")
    public ResponseEntity<CommonResponse> getWalletBalances(@PathVariable String hierarchyId) {
        log.info("Fetching wallet balances for hierarchyId: {}", hierarchyId);
        WalletBalanceResponseDto response = dashboardService.getWalletBalances(hierarchyId);
        return ResponseEntity.ok(new CommonResponse(201, "Wallet Balnce", response));
    }




}
