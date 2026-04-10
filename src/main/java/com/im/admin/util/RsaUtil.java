package com.im.admin.util;


import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

@Component
public class RsaUtil {

    private static final String PRIVATE_KEY =
                    "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC/N0Zyfw729cArvSR11N0vj+yjWNt7O5d9Z86ORb1PhazWd1qDnjhOC+gb8BOf/otDnWODMF0H6qdhQyOOlXtjHkOixSozz0NRzekSzKPYc7AzyqsyO/Y0qETKdoWcZVtHUQs/JFRXZ0cezm+IZeTNkI9Astc9YPv/k3FIEov8VTT/mwzO0ytiofOdq7Tt6hUcK94skrNbhhkdwBfWvjrX29bfDZSmQIk+jDLxw20TdRgNWfPndnaP73t3DLc4pLdErmdlhVfEAN87ony8okEHAF7bdGQYgJn8zmpjICxlTQTZavimPvDM9t72k1f5m+QxOnmZ9GX0pngnjKAovA9HAgMBAAECggEAPQdyV5bP+uhiplMe8PC4i7ae61+v5XwWlLLuDSCbFGdwy56aaAm2pMpOEaSlTtpaLoFsQnHML3uoYnWgQqDZGcCOC8Rr/M85CEH1Esf3v1zlLxKPY2AZ66KNGiWL7We4I2HtegCpXqAa5e0zUdBbn3R1FExAQFnU9DRl4n/w597o6NzdMI7AIQenaPsQHyi1wUp+cYoVlSEaBvGSNfE2lKdG81WhWhACnIrN++KKYJm9o1WoAS+5c+HFbOc0SWBoqENve0W2w6qRhR+dfDSJT94tn/CooRJADHvIvwNv/zw2qaAR52JwzUNAa475h7lHZUmvKSgY33AAQEsRPJDoYQKBgQDcUmWXwX2ONFzeHmL77PFoEdA9xg3q+qMQhJ3r5DvRjATr+88PiBEuHabioWqkD9FiZIUIX/A0RqRNoQREbVxYnPyB9/g63XvFc5yZKZhudSQhzOXlHn6z8JRXptjwKMkpWWGgc5xjqYNlbpCIvieelX07zct3KEsTvCuOFr5KUQKBgQDeLkUu+sGv1d8OODUQ2f4isUfQq4oRg+TRjBqhcjqWXH+S3uFM7uCdfjY+J7tdrYNlaI/aIK9JuFijVT+FWnG5cmTCAyTvc/XRdzuSxpNuCPA0znIxXHIqA2q7Zsmj3eG36Vrl0ffj+gvgczl92PuVSd9h3gmmYv6KOL5ALbLCFwKBgQDXVGCedSl1NpAdNq2fcvBtuhtAhLVNlzCQEE0YI7SjFKoqyayRjXbdD3XvwbssJZmLBJtQmBN3xCMJC/nTtt6ubKANloExZw506I2A1Dp6mlp5/nwh9i/3RYZ2NEeKURJqK0VTQCr07NPS6sCwJMksIQaI0JWvG95kp2AaOvg4QQKBgQCJi6OQ3fQiYwjfyLQxZoMJ/y4yZL/o7/osL8wcvpa+lyzzQ7++3PN5DLWP7RD1gYGiugS0SR+7R8C3/QKCD+fvon32DfRDuUopDuN2jjCfIjFyv4u6Oz0B/XOJ6KxUIfym2nRHaqFIJXhMz4h7/GRjm4QLFmZq90YVU+OT37aWqQKBgACNPtks6epfWzIMWt8WLyAm7KFT6BUlYNBer4qQ4EjIxQNWuKxlCBSV0tTL/KSMj7PHp6pUIaN06PRSqz3iNveKraGacfIOUmiyXvEYZTA71D5MBIFzH4K3nTXUmjzqkLZyGktVqXyA68+wwYFsY7dL9qWVbjP9qxBtlB5yPRW1";


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
