package com.im.admin.controller;

import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.HierarchyMasterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyMasterController {

    @Autowired
    private HierarchyMasterService hierarchyMasterService;

    @GetMapping("/name-code")
    public ResponseEntity<CommonResponse<List<HierarchyMasterDto>>> getNameAndCode() {
        log.info("API Call: getNameAndCode");
        List<HierarchyMasterDto> list = hierarchyMasterService.getAllNameAndCode();
        log.info("Fetched {} hierarchy name-code entries", list.size());
        return ResponseEntity.ok(new CommonResponse<>(200, "Hierarchy Name-Code fetched successfully", list));
    }

    @GetMapping("/hierarchy-details/{hierarchyCode}")
    public ResponseEntity<CommonResponse<List<HierarchyResponseDto>>> getHierarchyDetails(@PathVariable String hierarchyCode) {
        log.info("API Call: getHierarchyDetails for hierarchyCode: {}", hierarchyCode);
        List<HierarchyResponseDto> data = hierarchyMasterService.getHierarchyDetails(hierarchyCode);
        log.info("Fetched {} hierarchy detail entries for hierarchyCode: {}", data.size(), hierarchyCode);
        return ResponseEntity.ok(new CommonResponse<>(200, "Hierarchy Name-Code fetched successfully", data));
    }

}
