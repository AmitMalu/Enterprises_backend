package com.im.admin.controller;

import com.im.admin.dto.HierarchyMasterDto;
import com.im.admin.dto.HierarchyResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.HierarchyMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyMasterController {

    @Autowired
    private HierarchyMasterService hierarchyMasterService;

    @GetMapping("/name-code")
    public ResponseEntity<CommonResponse<List<HierarchyMasterDto>>> getNameAndCode() {
        List<HierarchyMasterDto> list = hierarchyMasterService.getAllNameAndCode();
        return ResponseEntity.ok(new CommonResponse<>(200, "Hierarchy Name-Code fetched successfully", list));
    }

    @GetMapping("/hierarchy-details/{hierarchyCode}")
    public ResponseEntity<CommonResponse<List<HierarchyResponseDto>>> getHierarchyDetails(@PathVariable String hierarchyCode) {
        List<HierarchyResponseDto> data = hierarchyMasterService.getHierarchyDetails(hierarchyCode);
        return ResponseEntity.ok(new CommonResponse<>(200, "Hierarchy Name-Code fetched successfully", data));
    }

}
