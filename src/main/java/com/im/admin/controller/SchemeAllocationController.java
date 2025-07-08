package com.im.admin.controller;

import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SchemeAllocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/scheme-allocation")
public class SchemeAllocationController {


    @Autowired
    private SchemeAllocationService allocationService;

    @PostMapping
    public ResponseEntity<CommonResponse<List<SchemeAllocation>>> allocateSchemes(@RequestBody SchemeAllocationRequestDto request) {
        CommonResponse<List<SchemeAllocation>> response = allocationService.allocateSchemes(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<String>> saveAllocation(
            @Valid @RequestBody SchemeAllocationRequest request) {
        CommonResponse<String> response = allocationService.saveSchemeAllocation(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
