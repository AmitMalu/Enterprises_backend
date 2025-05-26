package com.im.admin.controller;


import com.im.admin.dto.FetchSubProductFromRetailerRequest;
import com.im.admin.entities.SchemeAllocation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Validated
public class PaymentGatewayController {

//    @Autowired
//    private


//    @PostMapping("/fetch/sub-product")
//    public SchemeAllocation fetchSubProductFromProductCode(@RequestBody FetchSubProductFromRetailerRequest fetchSubProductFromRetailerRequest) {
//
//        SchemeAllocation schemeAllocationRespone =
//        return schemeAllocationRespone;
//    }

}
