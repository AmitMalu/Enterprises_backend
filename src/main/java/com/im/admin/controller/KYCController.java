package com.im.admin.controller;


import com.im.admin.dto.KYCRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.KYCResponseDto;
import com.im.admin.service.KYCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KYCController {

    @Autowired
    private KYCService kycService;

    @GetMapping("/fetch-documnets")
    public ResponseEntity<CommonResponse<KYCResponseDto>> fetchKYC(@RequestBody KYCRequestDto kycRequestDto) {
        KYCResponseDto dto = kycService.getKYCDetails(kycRequestDto);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new CommonResponse<>(201, "Documents fetched successfully", dto));
    }
}
