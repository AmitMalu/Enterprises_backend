package com.im.admin.config;

import com.im.admin.util.VidualPayCryptoUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author SHUBHAM KHOPADE
 */
@Configuration
public class CryptoConfig {

    @Bean
    public VidualPayCryptoUtil cryptoUtil() {

        return new VidualPayCryptoUtil(

                // secretKey / edKey
                "f37701ee0778cfe1dd97b7537ae71709",

                // saltKey / ivKey
                "bb13602c407b1ddd6506d59e410bafeb"
        );
    }
}
