package com.im.admin.controller;


import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.dto.RetailerTransactionResponseDto;
import com.im.admin.dto.SettlementPendingRequestDto;
import com.im.admin.response.BulkFundResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.FundingService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/funding")
public class FundingController {

    @Autowired
    private FundingService fundingService;

    @PostMapping("/credit-debit")
    public ResponseEntity<CommonResponse<String>> fundsCreditDebit(@RequestBody FundCreditDebitDto dto) {
        String result = fundingService.fundsCreditDebit(dto);
        return ResponseEntity.ok(CommonResponse.success(result, "Transaction processed successfully"));
    }

    @GetMapping("/bulk-credit-debit/sample")
    public void downloadSampleFile(HttpServletResponse response) throws IOException {
        fundingService.downloadSampleFile(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadBulkFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required.");
        }

        try {
            List<BulkFundResponse> responses = fundingService.processBulkFile(file);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while processing file: " + e.getMessage());
        }
    }

    @PostMapping("/settlement")
    public ResponseEntity<CommonResponse> getSettlementPending(
            @RequestBody SettlementPendingRequestDto request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<RetailerTransactionResponseDto> transactions =
                fundingService.getSettlementPendingTransactions(request, page, size);

        return ResponseEntity.ok(
                new CommonResponse(200, "Settlement pending transactions fetched successfully", transactions)
        );
    }

}
