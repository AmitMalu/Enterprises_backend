package com.im.admin.controller;



import com.im.admin.dto.DmtResponseDto;
import com.im.admin.entities.DmtBankList;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.DmtBankListRequest;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.service.DmtVendorService;
import com.im.admin.util.DmtVendorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/api/dmt")
public class DmtController {

    @Autowired
    private DmtOrchestrator dmtOrchestrator;

//    @Autowired
//    private DmtVendorService dmtVendorService;

    @Autowired
    private DmtVendorFactory vendorFactory;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String aesKey = "3445a761c284bd34";

    private static final String aesIv = "81afcb43311cc6a5";

    @PostMapping("/penny-drop")
    public CommonResponse<PaysprintPennyDropResponse> pennyDrop(@RequestBody PennyDropRequest request) {
        return dmtOrchestrator.pennyDropVerify(request);
    }


    @PostMapping("/send-otp")
    public CommonResponse<PaysprintSendOtpResponse> sendOtp(@RequestBody SendDmtRequest req) {
        return dmtOrchestrator.sendDmtOtp(req);
    }

    @PostMapping("/transaction")
    public CommonResponse<DmtResponseDto> processDmt(@RequestBody SendDmtRequest request, @RequestHeader Double latitude, @RequestHeader Double longitude) {
        return dmtOrchestrator.processDmt(request, latitude, longitude);
    }

//    @GetMapping("/paysprint/token")
//    public String generateToken() {
//        return dmtVendorService.generateJwtToken();
//    }

    @GetMapping("/paysprint/token")
    public String generateToken() {
        DmtVendorService vendorService = vendorFactory.getVendor("Paysprint DMT");
        return vendorService.generateJwtToken();
    }



    @PostMapping("/paysprint/encrypt")
    public Map<String, String> encrypt(@RequestBody Object request) {

        try {
            // Convert incoming JSON to String
            String json = objectMapper.writeValueAsString(request);

            // AES encrypt
            String encrypted = dmtOrchestrator.encrypt(
                    json,
                    aesKey,
                    aesIv
            );

            // Paysprint expected format
            return Collections.singletonMap("body", encrypted);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }


//    @GetMapping("/bank-list")
//    public ResponseEntity<CommonResponse<List<DmtBankList>>> getDmtBankList() {
//
//        List<DmtBankList> banks = dmtVendorService.getdmtbanklist();
//
//        if (banks.isEmpty()) {
//            return ResponseEntity
//                    .status(HttpStatus.NOT_FOUND)
//                    .body(CommonResponse.failure("Bank list not found"));
//        }
//
//        return ResponseEntity.ok(
//                CommonResponse.success(banks, "Bank list fetched successfully")
//        );
//    }

//    @GetMapping("/bank-list")
//    public ResponseEntity<CommonResponse<List<DmtBankList>>> getDmtBankList() {
//
//        DmtVendorService vendorService = vendorFactory.getVendor("Paysprint DMT");
//
//        List<DmtBankList> banks = vendorService.getdmtbanklist();
//
//        if (banks.isEmpty()) {
//            return ResponseEntity
//                    .status(HttpStatus.NOT_FOUND)
//                    .body(CommonResponse.failure("Bank list not found"));
//        }
//
//        return ResponseEntity.ok(
//                CommonResponse.success(banks, "Bank list fetched successfully")
//        );

        @PostMapping("/bank-list")
        public ResponseEntity<CommonResponse<List<DmtBankList>>> getDmtBankList(
                @RequestBody DmtBankListRequest request) {

            CommonResponse<List<DmtBankList>> response =
                    dmtOrchestrator.getBankList(request);

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response);
        }


    }



