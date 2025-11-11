//package com.im.admin.controller;
//
//
//
//import com.im.admin.request.SendDmtRequest;
//import com.im.admin.response.CommonResponse;
//import com.im.admin.service.PaysprintDmtService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/dmt")
//public class PaysprintDmtController {
//
//    @Autowired
//    private PaysprintDmtService paysprintDmtService;
//
//    @PostMapping("/process")
//    public CommonResponse<DmtResponseDto> processDmt(@RequestBody SendDmtRequest request) {
//        return paysprintDmtService.processDmt(request);
//    }
//}
