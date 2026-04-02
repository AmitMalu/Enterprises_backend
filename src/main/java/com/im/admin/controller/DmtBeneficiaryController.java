package com.im.admin.controller;


import com.im.admin.dto.DmtBeneficiaryDeleteResponse;
import com.im.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.im.admin.dto.DmtBeneficiaryFetchResponse;
import com.im.admin.entities.DmtBeneficiary;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.*;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DmtBeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficiary")
@RequiredArgsConstructor
public class DmtBeneficiaryController {

    @Autowired
    private DmtOrchestrator dmtOrchestrator;

    @Autowired
    private DmtBeneficiaryService dmtBeneficiaryService;

//    @PostMapping("/register")
//    public ResponseEntity<CommonResponse<BeneficiaryRegisterResponse>> registerBeneficiary(
//            @RequestBody BeneficiaryRegisterRequest request) throws Exception  {
//
//        CommonResponse<BeneficiaryRegisterResponse> response = dmtBeneficiaryService.registerBeneficiary(request);
//        return ResponseEntity.status(response.getStatusCode()).body(response);
//    }

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<BeneficiaryRegisterResponse>> registerBeneficiary(
            @RequestBody BeneficiaryRegisterRequest request) throws Exception  {

        CommonResponse<BeneficiaryRegisterResponse> response = dmtOrchestrator.registerBeneficiary(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

//    @PostMapping("/delete")
//    public ResponseEntity<CommonResponse<DmtBeneficiaryDeleteResponse>> deleteBeneficiary(
//            @RequestBody DmtBeneficiaryDeleteRequest request) throws Exception {
//        CommonResponse<DmtBeneficiaryDeleteResponse> response = dmtBeneficiaryService.deleteBeneficiary(request);
//        return ResponseEntity.status(response.getStatusCode()).body(response);
//    }

    @PostMapping("/delete")
    public ResponseEntity<CommonResponse<DmtBeneficiaryDeleteResponse>> deleteBeneficiary(
            @RequestBody DmtBeneficiaryDeleteRequest request) throws Exception {
        CommonResponse<DmtBeneficiaryDeleteResponse> response = dmtOrchestrator.deleteBeneficiary(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

//    @PostMapping("/fetchBeneficiary")
//    public ResponseEntity<CommonResponse<DmtBeneficiaryFetchResponse>> fetchBeneficiary(@RequestBody DmtBeneficiaryFetchRequest req) {
//        CommonResponse<DmtBeneficiaryFetchResponse> response = dmtBeneficiaryService.fetchBeneficiary(req);
//        return ResponseEntity.ok(response);
//    }

//    @PostMapping("/fetchBeneficiary")
//    public ResponseEntity<CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>>> fetchBeneficiary(
//            @RequestBody DmtBeneficiaryFetchRequest req,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> response =
//                dmtBeneficiaryService.fetchBeneficiary(req, page, size);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/fetchBeneficiary")
    public ResponseEntity<CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>>> fetchBeneficiary(
            @RequestBody DmtBeneficiaryFetchRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> response =
                dmtOrchestrator.fetchBeneficiary(req, page, size);
        return ResponseEntity.ok(response);
    }


//
//    @PostMapping("/fetchBeneficiaryByBeneId")
//    public ResponseEntity<CommonResponse<DmtBeneficiaryFetchByBeneIdResponse>> fetchBeneficiaryByBeneId(
//            @RequestBody DmtBeneficiaryFetchByBeneIdRequest req) {
//        CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> response = dmtBeneficiaryService.fetchBeneficiaryByBeneId(req);
//        return ResponseEntity.ok(response);
//    }


    @PostMapping("/fetchBeneficiaryByBeneId")
    public ResponseEntity<CommonResponse<DmtBeneficiaryFetchByBeneIdResponse>> fetchBeneficiaryByBeneId(
            @RequestBody DmtBeneficiaryFetchByBeneIdRequest req) {
        CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> response = dmtOrchestrator.fetchBeneficiaryByBeneId(req);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/update")
    public ResponseEntity<CommonResponse<DmtBeneficiaryResponseDto>> updateBeneficiary(@RequestBody BeneficiaryUpdateRequest request) {
        CommonResponse<DmtBeneficiaryResponseDto> response = dmtBeneficiaryService.updateBeneficiary(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    //instantPayDmt
    @PostMapping("/verifyBeneficiaryOtp")
    public ResponseEntity<CommonResponse<Object>> verifyBeneficiaryOtp(
            @RequestBody VerifyBeneOtpRequest req) throws Exception {

        CommonResponse<Object> response =
                dmtOrchestrator.verifyBeneficiaryOtp(req);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    //instantPayDmt
    @PostMapping("/delete/verify")
    public ResponseEntity<CommonResponse<Object>> verifyDeleteOtp(
            @RequestBody VerifyDeleteOtpRequest req) throws Exception {

        return ResponseEntity.ok(
                dmtOrchestrator.verifyDeleteOtp(req)
        );
    }

}
