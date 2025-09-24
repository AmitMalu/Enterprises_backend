package com.im.admin.config;

import com.im.admin.dto.EncryptedData;
import com.im.admin.util.AESEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.IvParameterSpec;
import java.util.HashMap;
import java.util.Map;

@Service
public class EncryptionService {


    @Value("${app.security.base-secret}")
    private String baseSecret;

    @Value("${app.security.pbkdf2-iterations}")
    private int iterations;

    @Value("${app.security.pbkdf2-keylength}")
    private int keyLength;

    public EncryptedData encryptCredentials(String plainText, String salt) throws Exception {
        return new EncryptedData("", "", "");
    }
}

