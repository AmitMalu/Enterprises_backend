package com.im.admin.controller;

import com.im.admin.dto.SchemeRateProductResponseDto;
import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.dto.SchemeSlabUpdateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.product.Products;
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

    @GetMapping("/get-scheme-details-by-scheme-code/{schemeCode}")
    public ResponseEntity<CommonResponse<SchemeDetailResponseDto>> getSchemeDetails(@PathVariable String schemeCode) {
        SchemeDetailResponseDto responseDto = schemeRateService.getSchemeDetails(schemeCode);
        return ResponseEntity.ok(CommonResponse.success(responseDto, "Scheme details fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<List<SchemeRate>> createSchemeRates(@RequestBody SchemeRateRequestDTO requestDTO) {
        return ResponseEntity.ok(schemeRateService.createSchemeRates(requestDTO));
    }

    @PutMapping("/update-slabs")
    public ResponseEntity<CommonResponse<SchemeDetailResponseDto>> updateSchemeSlabs(@RequestBody SchemeSlabUpdateRequestDTO requestDto) {
        SchemeDetailResponseDto updatedResponse = schemeRateService.updateSchemeRates(requestDto);
        return ResponseEntity.ok(CommonResponse.success(updatedResponse, "Slab rates updated successfully"));
    }

    @PostMapping("/get-product-details-by/{schemeCode}")
    public ResponseEntity<CommonResponse<SchemeRateProductResponseDto>> getProductBySchemeCode(@PathVariable("schemeCode") String schemeCode) {
        SchemeRateProductResponseDto productDetails = schemeRateService.getProductDetails(schemeCode);
        return ResponseEntity.ok(CommonResponse.success(productDetails, "Fetched Product Details By Scheme Code"));
    }

}
