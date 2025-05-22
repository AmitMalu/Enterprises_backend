package com.im.admin.controller;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
