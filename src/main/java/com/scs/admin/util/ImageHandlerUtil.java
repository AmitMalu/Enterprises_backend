package com.scs.admin.util;

import com.scs.admin.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
@ConfigurationProperties(prefix = "file")
public class ImageHandlerUtil {

    public void handleImageUpload(
            MultipartFile file,
            String fileLabel,
            String uploadFolder,
            Consumer<String> setPathMethod
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException(fileLabel + " is mandatory");
        }
        FileValidatorUtil.validateFile(file, fileLabel);
        String path = saveImage(file, uploadFolder);
        setPathMethod.accept(path);
    }


    public String saveImage(MultipartFile image, String folderName) throws IOException {
        String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();

        // Define a universal upload directory (creates folder in home directory)
        Path uploadDir = Paths.get(System.getProperty("user.home"), folderName).toAbsolutePath()
                .normalize();

        // Ensure the directory exists, otherwise create it
        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + uploadDir, e);
            }
        }

        // Save the file
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return the absolute file path (for storing in DB)
        return filePath.toString();
    }

    public void validateAllImagesMandatory(Map<String, MultipartFile> images) {
        List<String> missingImages = new ArrayList<>();

        images.forEach((label, file) -> {
            if (file == null || file.isEmpty()) {
                missingImages.add(label);
            }
        });

        if (!missingImages.isEmpty()) {
            String message = String.format("%d of %d images uploaded. Missing: %s",
                    images.size() - missingImages.size(),
                    images.size(),
                    String.join(", ", missingImages));
            throw new FileValidationException(message);
        }
    }


}
