package com.im.admin.controller;

import com.im.admin.service.impl.LocationUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author SHUBHAM KHOPADE
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/master")

public class LocationController {

    private final LocationUploadService service;

    @PostMapping(
            value="/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(

            @RequestParam MultipartFile file

    ) {

        service.upload(file);

        return ResponseEntity.ok(
                "Upload completed"
        );

    }

}
