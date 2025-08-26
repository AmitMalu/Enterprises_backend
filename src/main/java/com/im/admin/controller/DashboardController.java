package com.im.admin.controller;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.MyProfileResponseDto;
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
        log.info("API Call: getAdminDashboard");
        DashboardResponseDto response = dashboardService.getAdminDashboard();
        log.info("Admin Dashboard fetched successfully: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet-balance/{hierarchyId}")
    public ResponseEntity<CommonResponse> getWalletBalances(@PathVariable String hierarchyId) {
        log.info("API Call: getWalletBalances for hierarchyId: {}", hierarchyId);
        WalletBalanceResponseDto response = dashboardService.getWalletBalances(hierarchyId);
        log.info("Wallet Balance fetched: {}", response);
        return ResponseEntity.ok(new CommonResponse(201, "Wallet Balance", response));
    }

    @GetMapping("/profile/{hierarchyId}")
    public ResponseEntity<CommonResponse<MyProfileResponseDto>> getUserProfile(@PathVariable String hierarchyId) {
        log.info("API Call: getUserProfile for hierarchyId: {}", hierarchyId);
        MyProfileResponseDto profile = dashboardService.getUserProfile(hierarchyId);
        log.info("Profile fetched successfully: {}", profile);
        return ResponseEntity.ok(new CommonResponse<>(200, "Profile fetched successfully", profile));
    }



}
