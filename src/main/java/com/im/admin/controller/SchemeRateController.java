package com.im.admin.controller;

import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.service.SchemeRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheme-rate")
@Slf4j
public class SchemeRateController {

    @Autowired
    private SchemeRateService schemeRateService;

//    fetch all api is done in the scheme are column shown  --> id, scheme code, scheme name, product name, sub product name,
//    scheme rate then we will create in that we took scheme code , operator code and name and slabs rate this will we create
//    now user click on  any scheme so data should be fetch is under any scheme code there will  be multiple operator and and each
//    operator have multiple slabs or single slab will be there make api controller , service , service Impl

    @GetMapping("/{schemeCode}")
    public ResponseEntity<SchemeDetailResponseDto> getSchemeDetails(@PathVariable String schemeCode) {
        return ResponseEntity.ok(schemeRateService.getSchemeDetails(schemeCode));
    }

    @PostMapping
    public ResponseEntity<List<SchemeRate>> createSchemeRates(@RequestBody SchemeRateRequestDTO requestDTO) {
        return ResponseEntity.ok(schemeRateService.createSchemeRates(requestDTO));
    }






}
