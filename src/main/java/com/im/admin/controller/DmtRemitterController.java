package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.RemitterUpdateRequest;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.request.DmtOtpRequest;
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
public class DmtRemitterController {

    @Autowired
    private DmtOrchestrator dmtOrchestrator;

    @Autowired
    private  RemitterService remitterService;

//    @PostMapping("/check")
//    public ResponseEntity<CommonResponse<RemitterQueryResponse>> checkAndProcess(
//            @RequestBody RemitterQueryRequest request) throws Exception  {
//
//        CommonResponse<RemitterQueryResponse> response = remitterService.checkRemitter(request.getMobile(),request.getSubProductId());
//        return ResponseEntity.status(response.getStatusCode()).body(response);
//    }

//    dmtOrchestrator

    @PostMapping("/check")
    public ResponseEntity<CommonResponse<RemitterQueryResponse>> checkAndProcess(
            @RequestBody RemitterQueryRequest request) throws Exception {

        CommonResponse<RemitterQueryResponse> response =
                dmtOrchestrator.checkRemitter(
//                        request.getMobile(),
//                        request.getSubProductId()
                        request
                );

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

//    @PostMapping("/register")
//    public ResponseEntity<CommonResponse<RemitterRegisterResponse>> registerRemitter(
//            @RequestBody RemitterRegisterRequest request) {
//        CommonResponse<RemitterRegisterResponse> response =
//                (CommonResponse<RemitterRegisterResponse>) remitterService.registerRemitter(request);
//        return ResponseEntity.status(response.getStatusCode()).body(response);
//    }

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<RemitterRegisterResponse>> registerRemitter(
            @RequestBody RemitterRegisterRequest request) {

        CommonResponse<RemitterRegisterResponse> response =
                dmtOrchestrator.registerRemitter(request);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

//    @PostMapping("/ekyc")
//    public ResponseEntity<CommonResponse<Object>> doEkyc(@RequestBody RemitterEkycRequest request) {
//        CommonResponse<Object> response = remitterService.doEkyc(request);
//        return ResponseEntity.status(response.getStatusCode()).body(response);
//    }

    @PostMapping("/ekyc")
    public ResponseEntity<CommonResponse<Object>> doEkyc(@RequestBody RemitterEkycRequest request) {
        CommonResponse<Object> response = dmtOrchestrator.doEkyc(request);
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

    @PostMapping("/send-otp")
    public CommonResponse<String> sendOtp(@RequestBody DmtOtpRequest dmtOtpRequest) {
        return remitterService.sendOtp(dmtOtpRequest);
    }


    @PostMapping("/resend-otp")
    public CommonResponse<String> resendOtp(@RequestBody  DmtOtpRequest request) {
        return remitterService.resendOtp(request.getMobile());
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<CommonResponse<Object>> validateOtp(
            @RequestBody RemitterEkycRequest request) throws Exception {

        return ResponseEntity.ok(
                dmtOrchestrator.validateOtp(request)
        );
    }




}

