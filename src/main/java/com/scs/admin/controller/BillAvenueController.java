package com.scs.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.BillFetchRequest;
import com.scs.admin.request.BillPaymentRequest;
import com.scs.admin.request.BillerRequest;
import com.scs.admin.request.TransactionStatusRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.BillAvenueService;
import jakarta.servlet.http.HttpServletRequest;
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
    public CommonResponse<Object> billPayment(@RequestBody BillPaymentRequest request, HttpServletRequest httpServletRequest) throws Exception {

        RequestHeaderMetaDto requestHeaderMetaDtoMeta = RequestHeaderMetaDto.from(httpServletRequest);

        return (CommonResponse<Object>) billAvenueService.doBillPayment(request, requestHeaderMetaDtoMeta);
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
