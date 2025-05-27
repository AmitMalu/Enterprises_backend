package com.im.admin.controller;


import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/master")
public class MasterController {


    @PostMapping("/card-type")
    public ResponseEntity<CommonResponse<?>> cardType() {


        return null;
    }
}
