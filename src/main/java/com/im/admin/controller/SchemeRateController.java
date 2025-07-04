package com.im.admin.controller;

import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.response.CommonResponse;
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

    @GetMapping("/{schemeCode}")
    public ResponseEntity<CommonResponse<SchemeDetailResponseDto>> getSchemeDetails(@PathVariable String schemeCode) {
        SchemeDetailResponseDto responseDto = schemeRateService.getSchemeDetails(schemeCode);
        return ResponseEntity.ok(CommonResponse.success(responseDto, "Scheme details fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<List<SchemeRate>> createSchemeRates(@RequestBody SchemeRateRequestDTO requestDTO) {
        return ResponseEntity.ok(schemeRateService.createSchemeRates(requestDTO));
    }






}
