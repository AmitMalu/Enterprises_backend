package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.RemitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

