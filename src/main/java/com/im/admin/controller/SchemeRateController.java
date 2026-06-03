package com.im.admin.controller;

import com.im.admin.dto.SchemeRateProductResponseDto;
import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.dto.SchemeSlabUpdateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.product.Products;
import com.im.admin.request.SchemeRateRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.response.SchemeRateResponse;
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
        log.info("Received request to fetch scheme details for schemeCode: {}", schemeCode);
        SchemeDetailResponseDto responseDto = schemeRateService.getSchemeDetails(schemeCode);
        log.info("Successfully fetched scheme details for schemeCode: {}", schemeCode);
        return ResponseEntity.ok(CommonResponse.success(responseDto, "Scheme details fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<List<SchemeRate>> createSchemeRates(@RequestBody SchemeRateRequestDTO requestDTO) {
        log.info("Received request to create scheme rates: {}", requestDTO);
        List<SchemeRate> createdRates = schemeRateService.createSchemeRates(requestDTO);
        log.info("Successfully created {} scheme rates.", createdRates.size());
        return ResponseEntity.ok(createdRates);
    }

    @PutMapping("/update-slabs")
    public ResponseEntity<CommonResponse<SchemeDetailResponseDto>> updateSchemeSlabs(@RequestBody SchemeSlabUpdateRequestDTO requestDto) {
        log.info("Received request to update scheme slabs: {}", requestDto);
        SchemeDetailResponseDto updatedResponse = schemeRateService.updateSchemeRates(requestDto);
        log.info("Successfully updated slabs for schemeCode: {}", requestDto.getSchemeCode());
        return ResponseEntity.ok(CommonResponse.success(updatedResponse, "Slab rates updated successfully"));
    }

    @PostMapping("/get-product-details-by/{schemeCode}")
    public ResponseEntity<CommonResponse<SchemeRateProductResponseDto>> getProductBySchemeCode(@PathVariable("schemeCode") String schemeCode) {
        log.info("Received request to fetch product details for schemeCode: {}", schemeCode);
        SchemeRateProductResponseDto productDetails = schemeRateService.getProductDetails(schemeCode);
        log.info("Successfully fetched product details for schemeCode: {}", schemeCode);
        return ResponseEntity.ok(CommonResponse.success(productDetails, "Fetched Product Details By Scheme Code"));
    }

    @PostMapping("/find")
    public ResponseEntity<CommonResponse<SchemeRateResponse>> findSchemeRate(
            @RequestBody SchemeRateRequest request) {

        log.info("Received scheme rate lookup request. SchemeCode={}, Amount={}",
                request.getSchemeCode(),
                request.getAmount());

        CommonResponse<SchemeRateResponse> response =
                schemeRateService.getSchemeRate(request);

        return ResponseEntity.ok(response);
    }


}
