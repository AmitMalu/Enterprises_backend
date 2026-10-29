package com.scs.admin.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

public class ConvertImageUrlToBase64 {

    // Static utility method to convert image URL or local path to Base64
    public static String fromUrl(String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                return null;
            }

            byte[] bytes;

            // Check if path is URL
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                URL url = new URL(imagePath);
                try (InputStream inputStream = url.openStream()) {
                    bytes = inputStream.readAllBytes();
                }
            } else {
                // Treat as local file path
                File file = new File(imagePath);
                if (!file.exists()) {
                    System.out.println("File not found: " + imagePath);
                    return null;
                }
                bytes = Files.readAllBytes(file.toPath());
            }

            // Convert to Base64
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
