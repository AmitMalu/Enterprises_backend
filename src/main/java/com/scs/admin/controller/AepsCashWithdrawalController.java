package com.scs.admin.controller;

import com.scs.admin.request.AepsCashWithdrawalRequest;
import com.scs.admin.response.AepsCashWithdrawalResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AepsCashWithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aeps")
public class AepsCashWithdrawalController {

    @Autowired
    private AepsCashWithdrawalService cashWithdrawalService;

    @PostMapping("/cash-withdrawal")
    public ResponseEntity<CommonResponse<AepsCashWithdrawalResponse>> cashWithdrawal(
            @RequestBody AepsCashWithdrawalRequest req,
            @RequestHeader Double latitude, @RequestHeader Double longitude) {

        return ResponseEntity.ok(cashWithdrawalService.cashWithdrawal(req,latitude,longitude));
    }


}
