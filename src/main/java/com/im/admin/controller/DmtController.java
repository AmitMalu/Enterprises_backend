package com.im.admin.controller;



import com.im.admin.dto.DmtResponseDto;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dmt")
public class DmtController {

    @Autowired
    private DmtOrchestrator dmtOrchestrator;

    @PostMapping("/penny-drop")
    public CommonResponse<PaysprintPennyDropResponse> pennyDrop(@RequestBody PennyDropRequest request) {
        return dmtOrchestrator.pennyDropVerify(request);
    }


    @PostMapping("/send-otp")
    public CommonResponse<PaysprintSendOtpResponse> sendOtp(@RequestBody SendDmtRequest req) {
        return dmtOrchestrator.sendDmtOtp(req);
    }

    @PostMapping("/transaction")
    public CommonResponse<DmtResponseDto> processDmt(@RequestBody SendDmtRequest request) {
        return dmtOrchestrator.processDmt(request);
    }



}
