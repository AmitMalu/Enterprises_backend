package com.im.admin.config;

import com.im.admin.util.VidualPayCryptoUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the VidualPay crypto bean with the correct sandbox credentials.
 *
 * Key mapping:
 *   arg1 → encryptdecryptKey  (AES secret key / edKey)
 *   arg2 → saltKey            (GCM nonce / ivKey)
 *
 * DO NOT swap these — confirmed by VidualPay support.
 *
 * @author SHUBHAM KHOPADE
 */
@Configuration
public class CryptoConfig {

    @Bean
    public VidualPayCryptoUtil vidualPayCryptoUtil() {

        return new VidualPayCryptoUtil(

                // encryptdecryptKey  →  used as AES key (edKey)
                "47cdba1911f078afe863774a8dffcb26",

                // saltKey            →  used as GCM IV (ivKey)
                "bb13602c407b1ddd6506d59e410bafeb"
        );
    }
}
