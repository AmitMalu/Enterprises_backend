package com.scs.admin.controller;

import com.scs.admin.util.RequestIdUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("billavenue")
@RestController
public class RequestIdController {

    // GET endpoint to generate requestId
    @GetMapping("/generateRequestId")
    public String generateRequestId() {
        String requestId = RequestIdUtil.generate();
        return requestId; // returns plain text requestId
    }
}
