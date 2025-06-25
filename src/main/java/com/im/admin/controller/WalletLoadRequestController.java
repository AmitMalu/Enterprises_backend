package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.WalletLoadRequestDto;
import com.im.admin.entities.WalletLoadRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.WalletLoadRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/wallet-load")
@RequiredArgsConstructor
@Slf4j
public class WalletLoadRequestController {

    @Autowired
    private WalletLoadRequestService walletLoadRequestService;

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<WalletLoadRequest>> submitWalletLoadRequest(
            @RequestPart("data") @Valid String rawData,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            log.info("Entering Wallet Load Request method");

            ObjectMapper mapper = new ObjectMapper();
            WalletLoadRequestDto dto = mapper.readValue(rawData, WalletLoadRequestDto.class);
            CommonResponse<WalletLoadRequest> response = walletLoadRequestService.saveWalletLoadRequest(dto, imageFile);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to submit request: " + e.getMessage(), null));
        }
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


}

