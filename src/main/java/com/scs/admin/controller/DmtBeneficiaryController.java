package com.scs.admin.controller;


import com.scs.admin.dto.DmtBeneficiaryDeleteResponse;
import com.scs.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.scs.admin.dto.DmtBeneficiaryFetchResponse;
import com.scs.admin.entities.DmtBeneficiary;
import com.scs.admin.orchestrator.DmtOrchestrator;
import com.scs.admin.request.*;
import com.scs.admin.response.BeneficiaryRegisterResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.DmtBeneficiaryService;
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

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<BeneficiaryRegisterResponse>> registerBeneficiary(
            @RequestBody BeneficiaryRegisterRequest request) throws Exception  {

        CommonResponse<BeneficiaryRegisterResponse> response = dmtOrchestrator.registerBeneficiary(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<CommonResponse<DmtBeneficiaryDeleteResponse>> deleteBeneficiary(
            @RequestBody DmtBeneficiaryDeleteRequest request) throws Exception {
        CommonResponse<DmtBeneficiaryDeleteResponse> response = dmtOrchestrator.deleteBeneficiary(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/fetchBeneficiary")
    public ResponseEntity<CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>>> fetchBeneficiary(
            @RequestBody DmtBeneficiaryFetchRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        CommonResponse<Page<DmtBeneficiaryFetchResponse.BeneficiaryData>> response =
                dmtOrchestrator.fetchBeneficiary(req, page, size);
        return ResponseEntity.ok(response);
    }


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
    @PostMapping("/verify-otp")
    public ResponseEntity<CommonResponse<Object>> verifyBeneficiaryOtp(
            @RequestBody VerifyBeneOtpRequest req) throws Exception {

        CommonResponse<Object> response =
                dmtOrchestrator.verifyBeneficiaryOtp(req);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    //instantPayDmt
    @PostMapping("/delete-verify")
    public ResponseEntity<CommonResponse<Object>> verifyDeleteOtp(
            @RequestBody VerifyDeleteOtpRequest req) throws Exception {

        return ResponseEntity.ok(
                dmtOrchestrator.verifyDeleteOtp(req)
        );
    }

}
