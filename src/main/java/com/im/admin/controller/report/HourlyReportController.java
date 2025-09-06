package com.im.admin.controller.report;


import com.im.admin.entities.HourlyReportEntity;
import com.im.admin.repositories.HourlyReportRepository;
import com.im.admin.service.reports.HourlyReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class HourlyReportController {

    @Autowired
    private HourlyReportService hourlyReportService;

    @Autowired
    private HourlyReportRepository hourlyReportRepository;

    // List: returns rows for left panel (id, dated, timestamp, subject, fileName)
    @GetMapping("/list")
    public ResponseEntity<List<HourlyReportEntity>> list(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(hourlyReportService.listReportsByDate(date));
    }

    // Download by id (id is like "2025-09-06_01_transaction")
    @GetMapping("/download/{id}")
    public void download(@PathVariable String id, HttpServletResponse response) throws Exception {
        hourlyReportService.streamReportDownload(id, response);
    }

    // Optional: force-generate reports for a date (admin use)
    @PostMapping("/generate")
    public ResponseEntity<String> generateForDate(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            hourlyReportService.generateReportsForDate(date);
            return ResponseEntity.ok("Started generation for " + date);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

