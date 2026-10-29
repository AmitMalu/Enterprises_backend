package com.scs.admin.controller;


import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.CardTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/api/card-type")
public class CardTypeController {

    @Autowired
    private CardTypeService cardTypeService;

    @GetMapping("/download-sample-format")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        log.info("Request received to download sample card type upload template");

        ByteArrayResource resource = cardTypeService.generateSampleTemplate();

        log.info("Sample template generated successfully");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_card_type_upload_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<CommonResponse<String>> bulkUpload(@RequestParam("file") MultipartFile file) {
        log.info("Bulk upload request received. File name: {}, Size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            String result = cardTypeService.uploadCardTypes(file);
            log.info("Bulk upload completed successfully. Result: {}", result);
            return ResponseEntity.ok(CommonResponse.success("Bulk Upload Complete", result));
        } catch (Exception e) {
            log.error("Bulk upload failed for file: {}. Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(CommonResponse.failure("Upload failed: " + e.getMessage()));
        }
    }

}
