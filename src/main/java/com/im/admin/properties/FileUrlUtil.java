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

//        domain  name request mai mil gaya
//                hhtps://uat.decipay.in/enteprises/images/Retailer/b011fc5e-1b57-489f-ab3e-3.png
        //Retailer/b011fc5e-1b57-489f-ab3e-3.png
        //https://api.nfttech.in/enterprises/images/Retailer/b011fc5e-1b57-489f-ab3e-3b66d2e2298c_WhatsApp Image 2026-05-09 at 10.53.23 AM.jpeg
    }
}
