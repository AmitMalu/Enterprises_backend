package com.im.admin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/convert")
public class ImageBase64Controller {

    @PostMapping("/image-to-base64")
    public String convertToBase64(@RequestParam("file") MultipartFile file) throws IOException {

        // Read file bytes
        byte[] fileBytes = file.getBytes();

        // Convert to Base64 (local variable - Java Base64 encoder)
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        return base64;
    }
}
