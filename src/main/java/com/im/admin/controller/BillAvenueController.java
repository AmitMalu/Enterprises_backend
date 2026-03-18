package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.request.BillFetchRequest;
import com.im.admin.request.BillPaymentRequest;
import com.im.admin.request.BillerRequest;
import com.im.admin.request.TransactionStatusRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BillAvenueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/billavenue")
public class BillAvenueController {

    @Autowired
    private BillAvenueService billAvenueService;

    @PostMapping("/biller-info-bulk")
    public CommonResponse<Object> fetchBillerBulk(@RequestBody BillerRequest request) throws Exception {

        Object responseJson = billAvenueService.fetchBillerInfobulk(request);

        return CommonResponse.success(responseJson, "Biller info fetched successfully");
    }

    @PostMapping("/biller-info")
    public CommonResponse<Object> fetchBiller(@RequestBody BillerRequest request) throws Exception {

        Object responseJson = billAvenueService.fetchBillerInfo(request);

        return CommonResponse.success(responseJson, "Biller info fetched successfully");
    }

    @PostMapping("/bill-fetch")
    public CommonResponse<Object> billFetch(@RequestBody BillFetchRequest request) throws Exception {

        Object responseJson = billAvenueService.fetchBill(request);

        return CommonResponse.success(responseJson, "Bill fetched successfully");
    }


    @PostMapping("/bill-payment")
    public CommonResponse<Object> billPayment(@RequestBody BillPaymentRequest request, @RequestHeader Double latitude, @RequestHeader Double longitude) throws Exception {

        return (CommonResponse<Object>) billAvenueService.doBillPayment(request, latitude, longitude);
    }

    @PostMapping("/transaction-status")
    public CommonResponse<Object> fetchTransactionStatus(@RequestBody TransactionStatusRequest request) throws Exception {

        Object response =
                billAvenueService.fetchTransactionStatus(request);

        return CommonResponse.success(
                response,
                "Transaction status fetched successfully"
        );
    }




}
