package com.im.admin.controller;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.request.DashboardTransactionRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DashboardTransactionDto;
import com.im.admin.response.MyProfileResponseDto;
import com.im.admin.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<CommonResponse<WalletBalanceResponseDto>> getWalletBalances(@PathVariable String hierarchyId) {
        log.debug("Fetching wallet balance for hierarchyId: {}", hierarchyId);
        WalletBalanceResponseDto response = dashboardService.getWalletBalances(hierarchyId);
        log.info("Wallet Balance fetched: {}", response);
        return ResponseEntity.ok(
                new CommonResponse<>(201, "Wallet balance retrieved successfully", response));
    }

    @GetMapping("/profile/{hierarchyId}")
    public ResponseEntity<CommonResponse<MyProfileResponseDto>> getUserProfile(@PathVariable String hierarchyId) {
        log.info("API Call: getUserProfile for hierarchyId: {}", hierarchyId);
        MyProfileResponseDto profile = dashboardService.getUserProfile(hierarchyId);
        log.info("Profile fetched successfully: {}", profile);
        return ResponseEntity.ok(new CommonResponse<>(200, "Profile fetched successfully", profile));
    }

//    @PostMapping("/transactions")
//    public ResponseEntity<List<DashboardTransactionDto>> getDashboardTransactions(
//             @RequestBody DashboardTransactionRequest request) {
//
//        return ResponseEntity.ok(
//                dashboardService.getDashboardTransactions(request.getHierarchyId())
//        );
//    }


    @PostMapping("/data")
    public ResponseEntity<CommonResponse<Object>> getDashboardTransactions(
            @RequestBody DashboardTransactionRequest request) {

        Map<String, Object> responseData =
                dashboardService.getDashboardTransactions(request);

        return ResponseEntity.ok(
                CommonResponse.success(responseData, "Dashboard transactions fetched successfully")
        );
    }


}
