package com.im.admin.util;

import com.im.admin.properties.FileAccessProperties;
import com.im.admin.properties.FileStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class ImageUtil {

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Autowired
    private FileAccessProperties fileAccessProperties;

    public String saveImage(MultipartFile imageFile, String category) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) return null;

        Path categoryDir = Paths.get(fileStorageProperties.getBaseDir(), category);
        if (!Files.exists(categoryDir)) {
            Files.createDirectories(categoryDir);
        }

        String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        Path filePath = categoryDir.resolve(fileName);

        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileAccessProperties.getBaseUrl() + category + "/" + fileName;
    }
}
