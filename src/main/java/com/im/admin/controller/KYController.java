package com.im.admin.controller;


import com.im.admin.dto.HierarchySearchRequestDto;
import com.im.admin.dto.KYCEditRequest;
import com.im.admin.dto.KYCRequestDto;
import com.im.admin.entities.Retailer;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.KYCResponseDto;
import com.im.admin.response.KycDocumentDto;
import com.im.admin.service.KYCService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/kyc")
public class KYController {

    @Autowired
    private KYCService kycService;

    @Autowired
    private CommonMethods commonMethods;


    @PostMapping("/search")
    public ResponseEntity<CommonResponse<Page<KYCResponseDto>>> searchKycRecords(
            @RequestBody HierarchySearchRequestDto requestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<KYCResponseDto> responsePage;

        String searchKeyword = requestDto.getSearchKeyword();

        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            String hierarchyType = commonMethods.extractHierarchyType(searchKeyword);

            KYCResponseDto dto = kycService.searchByMobileNoAndHierarchyId(new KYCRequestDto(searchKeyword));

            if (dto == null) {
                return ResponseEntity.ok(new CommonResponse<>(200, "No record found", Page.empty(pageable)));
            }

            // Wrap single result in Page
            responsePage = new PageImpl<>(List.of(dto), pageable, 1);

        } else {
            // No search keyword, fetch hierarchy-wise paged data
            responsePage = kycService.searchByHierarchyWise(requestDto.getHierarchyType(), pageable);
        }

        return ResponseEntity.ok(new CommonResponse<>(200, "KYC records fetched successfully", responsePage));
    }

    @PostMapping("/details/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<KycDocumentDto>>> getKycDocumentsByHierarchyId(
            @PathVariable String hierarchyId) {

        List<KycDocumentDto> documents = kycService.getKycDetailDocumentsByHierarchyId(hierarchyId);

        if (documents.isEmpty()) {
            return ResponseEntity.ok(new CommonResponse<>(200, "No record found", Collections.emptyList()));
        }

        return ResponseEntity.ok(new CommonResponse<>(200, "KYC documents fetched successfully", documents));
    }




    @GetMapping("/fetch-documnets")
    public ResponseEntity<CommonResponse<KYCResponseDto>> searchByMobileNoAndHierarchyId(@RequestBody KYCRequestDto kycRequestDto) {
        KYCResponseDto dto = kycService.searchByMobileNoAndHierarchyId(kycRequestDto);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new CommonResponse<>(201, "Documents fetched successfully", dto));
    }

    @GetMapping("/search-by-hierarchy-wise")
    public ResponseEntity<CommonResponse<Page<KYCResponseDto>>> searchByHierarchyWise(
            @RequestBody HierarchySearchRequestDto requestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<KYCResponseDto> responsePage = kycService.searchByHierarchyWise(requestDto.getHierarchyType(), pageable);

        return ResponseEntity.ok(new CommonResponse<>(201, "fetched all Documents", responsePage));
    }


    @PostMapping(value = "/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<String>> editKyc(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        KYCEditRequest request;
        try {
            request = JsonUtil.parseJson(requestJson, KYCEditRequest.class);
        } catch (Exception e) {
            CommonResponse<String> errorResponse = new CommonResponse<>();
            errorResponse.setStatusCode(HttpStatus.BAD_REQUEST.value());
            errorResponse.setMessage("Invalid request JSON");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String result = kycService.editKyc(request, imageFile);

        CommonResponse<String> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage(result);
        response.setData(null);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete-document")
    public ResponseEntity<CommonResponse<Object>> deleteKycDocument(
            @RequestParam String hierarchyId,
            @RequestParam String category) {

        kycService.deleteKycDocument(hierarchyId, category);

        CommonResponse<Object> commonResponse = new CommonResponse<>(201, "Document deleted (nullified) successfully", null);
        return ResponseEntity.ok(commonResponse);
    }


    @PostMapping("/approved-documents")
    public ResponseEntity<CommonResponse<Object>> getApprovedDocuments(
            @RequestBody HierarchySearchRequestDto requestDto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Object result = kycService.getApprovedDocuments(requestDto.getSearchKeyword(), pageable);

        CommonResponse<Object> response = new CommonResponse<>(
                20, "Approved document(s) fetched successfully", result
        );

        return ResponseEntity.ok(response);
    }

}
