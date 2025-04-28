package com.im.admin.controller;

import com.im.admin.dto.WalletLoadRequestDto;
import com.im.admin.service.WalletLoadRequestService;
import lombok.RequiredArgsConstructor;
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
public class WalletLoadRequestController {

    @Autowired
    private WalletLoadRequestService walletLoadRequestService;

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitWalletLoadRequest(
            @RequestPart("data") WalletLoadRequestDto dto,
            @RequestPart("imageFile") MultipartFile imageFile){
        try {
            String message = walletLoadRequestService.saveWalletLoadRequest(dto, imageFile);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit request: " + e.getMessage());
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

