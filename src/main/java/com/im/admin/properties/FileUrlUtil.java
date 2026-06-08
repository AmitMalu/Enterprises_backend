package com.im.admin.properties;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
@RequiredArgsConstructor
public class FileUrlUtil {

    private final FileAccessProperties fileAccessProperties;

    public String getFullUrl(String relativePath) {

        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        return fileAccessProperties.getBaseUrl() + relativePath;
    }
}
