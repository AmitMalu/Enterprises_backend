package com.im.admin.controller;

import com.im.admin.dto.RemitterEkycRequest;
import com.im.admin.dto.RemitterQueryRequest;
import com.im.admin.dto.RemitterRegisterRequest;
import com.im.admin.service.RemitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remitter")
@RequiredArgsConstructor
public class RemitterController {

    @Autowired
    private  RemitterService remitterService;

    @PostMapping("/check")
    public ResponseEntity<?> checkAndProcess(@RequestBody RemitterQueryRequest request){
        return ResponseEntity.ok(remitterService.checkRemitter(request.getMobile()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerRemitter(@RequestBody RemitterRegisterRequest request){
        return ResponseEntity.ok(remitterService.registerRemitter(request));
    }

    @PostMapping("/ekyc")
    public ResponseEntity<?> doEkyc(@RequestBody RemitterEkycRequest request){
        return ResponseEntity.ok(remitterService.doEkyc(request));
    }
}

