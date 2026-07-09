package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.dto.PrefundRequestResponseDTO;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.entities.PrefundRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PrefundRequestService;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/api/prefund")
@RequiredArgsConstructor
@Slf4j
public class  PrefundRequestController {

    @Autowired
    private PrefundRequestService prefundRequestService;

    @Autowired
    private CommonMethods commonMethods;

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PrefundRequest>> submitWalletLoadRequest(
            @RequestPart("data") @Valid String rawData,
            @RequestPart("imageFile") MultipartFile imageFile,
            HttpServletRequest httpServletRequest) {
        try {
            log.info("Entering Wallet Load Request method with data: {}", rawData);

            RequestHeaderMetaDto requestHeaderMetaDtoMeta = RequestHeaderMetaDto.from(httpServletRequest);

            long startTime = System.currentTimeMillis();
            log.info("======= START: createOrder =======");
            log.info("Request: {}", rawData);
            log.info("Location: Latitude={}, Longitude={}", requestHeaderMetaDtoMeta.getLatitude(), requestHeaderMetaDtoMeta.getLongitude());
            log.info("IP Address: {}", requestHeaderMetaDtoMeta.getIpAddress());

            // Validate IP address format
            if (!commonMethods.isValidIpAddress(requestHeaderMetaDtoMeta.getIpAddress())) {
                log.error("Invalid IP address format: {}", requestHeaderMetaDtoMeta.getIpAddress());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error(400, "Invalid IP address format. Expected format: 1.22.36.186"));
            }

            // Validate location
            if (requestHeaderMetaDtoMeta.getLatitude() == null || requestHeaderMetaDtoMeta.getLongitude() == null) {
                log.error("Missing location headers");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error(400, "Latitude and Longitude are required"));
            }

            ObjectMapper mapper = new ObjectMapper();
            PrefundRequestDto dto = mapper.readValue(rawData, PrefundRequestDto.class);

            CommonResponse<PrefundRequest> response = prefundRequestService.saveWalletLoadRequest(dto, imageFile, requestHeaderMetaDtoMeta);
            log.info("Wallet Load Request submitted successfully for mobileNumber: {}", dto.getMobileNumber());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation failed for Wallet Load Request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Failed to submit Wallet Load Request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to submit request: " + e.getMessage(), null));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<Page<PrefundRequestResponseDTO>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String requestedBy,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) Double depositAmount,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate depositDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy")LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate toDate,
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
        log.info("Fetching all Prefund Requests with filters - mobileNumber: {}, status: {}", mobileNumber, status);
        Page<PrefundRequestResponseDTO> pagedResult = prefundRequestService.getAllPrefundRequests(
                page, size, id, requestedBy, mobileNumber, depositAmount, status,
                depositDate,fromDate, toDate, createDateTime, bankHolderName, bankAccountName,
                bankAccountNumber, bankTranId, paymentMode, narration, ipAddress,
                approveOrRejectDate, approveOrRejectTime, remarks, depositType
        );

        CommonResponse<Page<PrefundRequestResponseDTO>> response = new CommonResponse<>(
                200, "Prefund requests fetched successfully", pagedResult
        );

        log.info("Fetched {} Prefund Requests", pagedResult.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/action")
    public ResponseEntity<CommonResponse<String>> takeActionOnFundRequest(@RequestBody FundRequestActionDto actionDto) {
        CommonResponse<String> response = prefundRequestService.takeActionOnFundRequest(actionDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        log.info("Fetching image for filename: {}", filename);
        Path filePath = Paths.get("uploads/wallet-images").resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            log.info("Image found: {}", filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // or detect dynamically
                    .body(resource);
        } else {
            log.error("Image not found: {}", filename);
            throw new FileNotFoundException("Image not found: " + filename);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PrefundRequestResponseDTO>> getPrefundRequestById(@PathVariable Long id) {

        log.info("Fetching Prefund Request by ID: {}", id);

        CommonResponse<PrefundRequestResponseDTO> response =
                prefundRequestService.getById(id);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}

