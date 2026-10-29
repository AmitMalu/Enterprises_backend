package com.scs.admin.util;


import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

@Component
public class RsaUtil {

    private static final String PRIVATE_KEY =
            "MIIEuwIBADANBgkqhkiG9w0BAQEFAASCBKUwggShAgEAAoIBAQC40SMbrd3H/93CNbz79ZOXBOiec4xeitwzxILRC4WR7TWnJ025iIynBneQaR1N7IyGi5EI6oZKUJ2+XpKSZSgbyaAasL6wN7rsyC6Q4GCWflk1DJTHDVglsNW0C/mHNDq2ha3RU+hkDdG2pfJTxTyjOu1h9BnuvV/2mD+rO4RdLQ9566Q/t2lxHV7s3OR0+5Mj1+UhZEQJDS2WWIQH6AR3XklWeuurPdUSFoNdXcg8+aHOsocmAB7BOUPAcKakdNZLeciwDegKMBctr7RvLlfuk6nHHSssTAAqPzObUZglwbefb6gIrwiOgBxZipZv5DCxCZxKEDDVhvKJn4ScdX1VAgMBAAECggEAEErzluGVfyep05CsN+QW3kXjAfD6y2zfa7rjs20aGdq5E6LN4dDNYX4a9UKnADMd0lCkeQ4/x3Zi6aDpt/kt94EnWaMCNIO9OVmObVwyd6Hs2FXjGcct4ai8tjImDcrHcEbJo34LqyJivgDUI5lsNpGf0rZTwO2184XJex10lkgXwvEA4i1QkjCRm147/dEm1oonWd73ooGDQ1KUjfWSI1X+tYn6U+wi3yykJLgRlmMzi7DJstQ/rXXDiaG8u6Hecn/0oIzMu++RboGeH7NDXxsFosxFzQ/JeVcTeDnp9QFUV8oCWZJbKktk+/PfPDaWMitatWwHFp+uLkfWcX05cQKBgQDkcHtJnEfspktdy+KWnBW6dme+J+13F/gJyN3xdr6sXQ9SGkBdq//SC3A6dZD31J6Kt+qxNa/637psZVc0oYS0aV4JtHn0yXW0kJUAgFmtieMLd+mNeKQ2CBrd7k+rlkCXPRFfitR2yZMDYJPnOH2NbohU8jcYGLxYWymrK9dCWQKBgQDPHVgrLLLT0QonalFKXd2CYCmBTIfnZVhS455WBBLO7HbMXS/cGPc2vnI05+EYK1m94D4agMPxjss1ldeQCYN1gG6C2qMGbbYX0L7GwCIYBYoO4u9DhDfF1GXr8G4ZLyPWwhsxIwJwEKB5OGCZR35H8rNmA/WFWzWTKPy9nvsbXQJ/NYonWxlFwjE3eMRQVvw+G3Wk2uyAu2jiuWhanD04cy1caeG9NDcbvh7wLsNfNraeceSpHN94X1QQXVdjGPLCJ4aKg7vI2ZZd//Pi+DscyY+HpyhzlLSxx/geU+WGxrBwcwHdEId5RF36FtCL2aVjx8IgMciTIRKrEgpLudKMoQKBgQDGLjgNqAm9sQrcSFlcpZCtjkJo3Mg2XL01I2SAvgkZQx8x0gFXw/PZyEYT5cV3kROgIpCKX6ge3hoIPkc0JkZKCwiz4W2ZWyWgiDfkijiVb8d5EyELI9sSS3jzWO6g3oCWoMnQLMXKYb6L/6w/Py/QvKV4+lTWtD4BgHjHIPb6aQKBgDABrroP/eazk1c7VvPhNltjlEWJc1bWwJpio5IrqUUBLoq4C8cvV9y9W3XmqNTI0lSjwI8vw+ZH6ZdB2+KXJeUJM5VTqzB+Yu9I0uCgnAv5Y5aoyMDNgiql3AdGHi4tOtAZolHSpsG1p7CXN0LEAyu6O8HkVyVeyFDQkaVZU5w6";


    private PrivateKey getPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(PRIVATE_KEY);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    public String decrypt(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        return new String(cipher.doFinal(decoded));
    }

}
