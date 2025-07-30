package com.im.admin.controller;


import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.service.PayoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/payout")
@Slf4j
@RestController
public class PayoutController {

    @Autowired
    private PayoutService payoutService;

    LocalDateTime currentDateTimeDate = LocalDateTime.now();

    @PostMapping("/generate-token")
    public ResponseEntity<String> generateToken() {
        log.info("******* generateToken at {} started *******", currentDateTimeDate);
        String token = payoutService.generateToken();
        log.info("******* generateToken at {} ends *******", currentDateTimeDate);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @PostMapping("/send-payout-request")
    public ResponseEntity<String> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");
        String response = payoutService.sendPayoutRequest(sendPayoutRequest);
        log.info("======= sendPayoutRequest completed at {} =======", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-callback-url")
    public ResponseEntity<CommonResponse<PayoutMerchantCallbackResponse>> updateCallbackUrl(@RequestBody PayoutMerchantCallbackRequest request) {
        PayoutMerchantCallbackResponse responseDTO = payoutService.updateMerchantCallbackUrl(request);
        return ResponseEntity.ok(new CommonResponse<>(200, "Callback URL updated", responseDTO));
    }

    @PostMapping("/callback")
    public ResponseEntity<String> payoutCallback(@RequestBody Map<String, Object> payload) {
        String response = payoutService.handlePayoutCallback(payload);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-beneficiary")
    public ResponseEntity<CommonResponse<BeneficiaryBankAccount>> save(@RequestBody BeneficiaryBankAccountDTO dto) {
        return ResponseEntity.ok(payoutService.saveBeneficiary(dto));
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<Page<BeneficiaryBankAccount>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(payoutService.getAllFiltered(search, pageable));
    }


    @DeleteMapping("/delete-beneficiaries")
    public ResponseEntity<CommonResponse<String>> deleteMultiple(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(payoutService.deleteMultipleBeneficiaries(ids));
    }

}
