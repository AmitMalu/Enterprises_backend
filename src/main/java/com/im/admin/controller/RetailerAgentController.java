package com.im.admin.controller;

import com.im.admin.entities.RetailerUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.RetailerAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/retailer")
public class RetailerAgentController {

    @Autowired
    private RetailerAgentService retailerAgentService;

    @PostMapping("/create-agent")
    public ResponseEntity<CommonResponse<RetailerUser>> createAgent(@RequestBody RetailerUser retailerUser) {
        log.info("Received request to create Retailer Agent: {}", retailerUser);

        RetailerUser savedAgent = retailerAgentService.createAgent(retailerUser);

        log.info("Retailer Agent created successfully with ID: {}");

        CommonResponse<RetailerUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Retailer Agent created successfully");
        response.setData(savedAgent);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
