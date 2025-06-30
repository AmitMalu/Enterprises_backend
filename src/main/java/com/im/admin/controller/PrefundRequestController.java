package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.entities.PrefundRequest;
import com.im.admin.entities.product.Products;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PrefundRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prefund")
@RequiredArgsConstructor
@Slf4j
public class PrefundRequestController {

    @Autowired
    private PrefundRequestService prefundRequestService;

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PrefundRequest>> submitWalletLoadRequest(
            @RequestPart("data") @Valid String rawData,
            @RequestPart("imageFile") MultipartFile imageFile) {
        try {
            log.info("Entering Wallet Load Request method");

            ObjectMapper mapper = new ObjectMapper();
            PrefundRequestDto dto = mapper.readValue(rawData, PrefundRequestDto.class);
            CommonResponse<PrefundRequest> response = prefundRequestService.saveWalletLoadRequest(dto, imageFile);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to submit request: " + e.getMessage(), null));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<Page<PrefundRequest>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) Double depositAmount,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate depositDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createDateTime,
            @RequestParam(required = false) String bankHolderName,
            @RequestParam(required = false) String bankAccountName,
            @RequestParam(required = false) String bankAccountNumber,
            @RequestParam(required = false) String bankTranId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String narration,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate approveOrRejectDate,
            @RequestParam(required = false) String approveOrRejectTime,
            @RequestParam(required = false) String remarks,
            @RequestParam(required = false) String depositType
    ) {
        Page<PrefundRequest> pagedResult = prefundRequestService.getAllPrefundRequests(
                page, size, id, hierarchyId, mobileNumber, depositAmount, status,
                depositDate, createDateTime, bankHolderName, bankAccountName,
                bankAccountNumber, bankTranId, paymentMode, narration, ipAddress,
                approveOrRejectDate, approveOrRejectTime, remarks, depositType
        );

        CommonResponse<Page<PrefundRequest>> response = new CommonResponse<>(
                200, " prefund requests fetched successfully", pagedResult
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/action")
    public ResponseEntity<CommonResponse<String>> takeActionOnFundRequest(@RequestBody FundRequestActionDto actionDto) {
        CommonResponse<String> response = prefundRequestService.takeActionOnFundRequest(actionDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Path filePath = Paths.get("uploads/wallet-images").resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // or detect dynamically
                    .body(resource);
        } else {
            throw new FileNotFoundException("Image not found: " + filename);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PrefundRequest>> getPrefundRequestById(@PathVariable Long id) {
        CommonResponse<PrefundRequest> response = prefundRequestService.getById(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


}

