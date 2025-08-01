package com.im.admin.controller;

import com.im.admin.entities.RetailerAgent;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.RetailerAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retailer")
public class RetailerAgentController {

    @Autowired
    private RetailerAgentService retailerAgentService;

    @PostMapping("/create-agent")
    public ResponseEntity<CommonResponse<RetailerAgent>> createAgent(@RequestBody RetailerAgent agentRequest) {
        RetailerAgent savedAgent = retailerAgentService.createAgent(agentRequest);

        CommonResponse<RetailerAgent> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Retailer Agent created successfully");
        response.setData(savedAgent);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
