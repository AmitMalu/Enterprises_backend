package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.dto.RemitterUpdateRequest;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DmtRemitterResponseDto;
import com.im.admin.service.RemitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/remitter")
@RequiredArgsConstructor
public class RemitterController {

    @Autowired
    private  RemitterService remitterService;

    @PostMapping("/check")
    public ResponseEntity<CommonResponse<RemitterQueryResponse>> checkAndProcess(
            @RequestBody RemitterQueryRequest request) {

        CommonResponse<RemitterQueryResponse> response = remitterService.checkRemitter(request.getMobile(),request.getSubProductId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<RemitterRegisterResponse>> registerRemitter(
            @RequestBody RemitterRegisterRequest request) {
        CommonResponse<RemitterRegisterResponse> response =
                (CommonResponse<RemitterRegisterResponse>) remitterService.registerRemitter(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/ekyc")
    public ResponseEntity<CommonResponse<Object>> doEkyc(@RequestBody RemitterEkycRequest request) {
        CommonResponse<Object> response = remitterService.doEkyc(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/fetch-details/{mobileNo}")
    public CommonResponse<DmtRemitterResponseDto> fetchRemmiterDetails(@PathVariable("mobileNo") String mobileNumber) {
        return remitterService.fetchRemitterDetails(mobileNumber);
    }

    @PutMapping("/update")
    public ResponseEntity<CommonResponse<DmtRemitterResponseDto>> updateRemitter(
            @RequestBody RemitterUpdateRequest request) {

        CommonResponse<DmtRemitterResponseDto> response = remitterService.updateRemitter(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


}

