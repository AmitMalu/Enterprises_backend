package com.scs.admin.controller;

import com.scs.admin.request.ReportReceiptRequestDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.ReportReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportReceiptController {

    @Autowired
   private ReportReceiptService reportReceiptService;

    @PostMapping("/report-receipt")
    public ResponseEntity<CommonResponse<?>> reportReceipt(
            @RequestBody ReportReceiptRequestDto request) {

        Object receipt = reportReceiptService.getReceipt(request);

        return ResponseEntity.ok(
                CommonResponse.success(receipt, "Receipt fetched successfully")
        );
    }
}

