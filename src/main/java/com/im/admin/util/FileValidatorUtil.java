package com.im.admin.util;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Configuration
public class FileValidatorUtil {

    private static final long MAX_FILE_SIZE = (long) (1.5 * 1024 * 1024); // 1.5 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "pdf");

    public static void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " file is missing or empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(fieldName + " file size exceeds the maximum allowed size (1.5 MB).");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasAllowedExtension(originalFilename)) {
            throw new IllegalArgumentException(fieldName + " file must be of type: " + ALLOWED_EXTENSIONS);
        }
    }

    private static boolean hasAllowedExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}

