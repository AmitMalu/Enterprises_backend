package com.scs.admin.controller;

import com.scs.admin.dto.HierarchyMasterDto;
import com.scs.admin.dto.HierarchyResponseDto;
import com.scs.admin.request.HierarchyResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.HierarchyAccessService;
import com.scs.admin.service.HierarchyMasterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
