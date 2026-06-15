package com.im.admin.util;

/**
 * @author SHUBHAM KHOPADE
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class VidualPayCryptoUtil {

    private static final String ALGORITHM =
            "AES/GCM/NoPadding";

    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec keySpec;

    private final byte[] iv;

    private static final String edKey = "8086289ecab4a664c8410a1e79d19090";
    private static final String ivKey = "7cca987fb6cfeea37022977bde6e62c0";


    public VidualPayCryptoUtil(
            String edKey,
            String ivKey
    ) {

        byte[] key =
                edKey.getBytes(
                        StandardCharsets.UTF_8
                );

        if (key.length != 16
                && key.length != 24
                && key.length != 32) {

            throw new IllegalArgumentException(
                    "Key length must be 16, 24, or 32 bytes"
            );
        }

        this.keySpec =
                new SecretKeySpec(
                        key,
                        "AES"
                );

        this.iv =
                ivKey.getBytes(
                        StandardCharsets.UTF_8
                );
    }

    public String encrypt(
            String plainText
    ) {

        try {

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(
                            TAG_LENGTH,
                            iv
                    )
            );

            byte[] encrypted =
                    cipher.doFinal(
                            plainText.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64
                    .getEncoder()
                    .encodeToString(
                            encrypted
                    );

        } catch (Exception ex) {

            log.error(
                    "Encryption failed",
                    ex
            );

            throw new RuntimeException(
                    "Encrypt failed",
                    ex
            );
        }
    }

//    public String decrypt(
//            String encryptedText
//    ) {
//
//        try {
//
//            Cipher cipher =
//                    Cipher.getInstance(
//                            ALGORITHM
//                    );
//
//            cipher.init(
//                    Cipher.DECRYPT_MODE,
//                    keySpec,
//                    new GCMParameterSpec(
//                            TAG_LENGTH,
//                            iv
//                    )
//            );
//
//            byte[] decrypted =
//                    cipher.doFinal(
//                            Base64
//                                    .getDecoder()
//                                    .decode(
//                                            encryptedText
//                                    )
//                    );
//
//            return new String(
//                    decrypted,
//                    StandardCharsets.UTF_8
//            );
//
//        } catch (Exception ex) {
//
//            log.error(
//                    "Decryption failed",
//                    ex
//            );
//
//            throw new RuntimeException(
//                    "Decrypt failed",
//                    ex
//            );
//        }
//    }

//    public static void main(String[] args) throws JsonProcessingException {
//
//        String apiResponse =
//                """
//                                {
//                                  "successStatus": true,
//                                  "message": "Success",
//                                  "responseCode": "000",
//                                  "data": "9fyvLgza94v0ls7gU55OGMMx6+rocl+VU2n1ED19ti2n5Puw/zkTa2VwiKPlHO5MWhtFxHvZ+zLbY5pfotJuBVX3UdKscc3Ocmgi5gM/dAXF2wZ+zoCfc3Bv/et1wItHXsrwW4dYv2BVwSxM0FnXW66W3R0FZFaKNWHcCKvU4+9cDeQFZGGf2uq7VHZ2LovhW48habzhOfD+G2mfcNHyuOdtU3xLeSAbhDLu/GxOlDJYo1p22Wi9AQirCPTfPY6l+b9jbu8WorkwCX09Oo+MIRjkBJ2cR7sVM68EFxiYhCNPkSwM0iPNVeg6EZYoQuQatly0k8ySa5UYU5XL0gquCzXWFjC8v22XValbHHBC8ey48QGByDdZfpkaYKpHAKALL9FF4XQCNIM8qleishnoUhw49H32CEWR+2d5ymGg5Pml0GAUC/Ztv/9T0MU6RP/xOXRbh4wqLHhVaQ2Lcq7r+YS0ufgBNdYUPVsCLWPEAqBXU0aXiXcotvZgQrXL0YIqf9KcXjBetFO3hMXEJlA0dxQG6FqJYcYbtzTyw/ErobAAmbBuAbcSFQ9HXTxnaV5FHDrnLGRfEErKJtUW"
//                                }
//                        """;
//
//        ObjectMapper mapper =
//                new ObjectMapper();
//
//        JsonNode root =
//                mapper.readTree(
//                        apiResponse
//                );
//
//        String encrypted =
//                root.get("data")
//                        .asText();
//
//
//        VidualPayCryptoUtil crypto =
//                new VidualPayCryptoUtil(
//
//                        // IMPORTANT CHANGE
//                        "f37701ee0778cfe1dd97b7537ae71709",
//
//                        // saltKey
//                        "bb13602c407b1ddd6506d59e410bafeb"
//                );
//
//        String decrypted =
//                crypto.decrypt(
//                        encrypted
//                );
//
//        System.out.println(
//                decrypted
//        );
//    }

    public static void main(String[] args) {
        // Replace this with the encrypted text obtained from the encryption code
        String encryptedText =
                "9fyvLgza94v0ls7gU55OGMMx6+rocl+VU2n1ED19ti2n5Puw/zkTa2VwiKPlHO5MWhtFxHvZ+zLbY5pfotJuBVX3UdKscc3Ocmgi5gM/dAXF2wZ+zoCfc3Bv/et1wItHXsrwW4dYv2BVwSxM0FnXW66W3R0FZFaKNWHcCKvU4+9cDeQFZGGf2uq7VHZ2LovhW48habzhOfD+G2mfcNHyuOdtU3xLeSAbhDLu/GxOlDJYo1p22Wi9AQirCPTfPY6l+b9jbu8WorkwCX09Oo+MIRjkBJ2cR7sVM68EFxiYhCNPkSwM0iPNVeg6EZYoQuQatly0k8ySa5UYU5XL0gquCzXWFjC8v22XValbHHBC8ey48QGByDdZfpkaYKpHAKALL9FF4XQCNIM8qleishnoUhw49H32CEWR+2d5ymGg5Pml0GAUC/Ztv/9T0MU6RdHLOHdhh4wqLHhVaQ2Lcq7r+YS0ufgBNdYUPVsCLWPEAqBXU0aXiXcotvZgQrXL0YIqf6SWeyZ9jGC4mNTiFGF+PkoC/UCUZOgJs0P454MwsYkblOtwFLwHRg5SD038w5ugrIRgDL+HoBQVAuv+";
        // Decrypt the ciphertext
        String decryptedText = decrypt(encryptedText);
        System.out.println("Decrypted text:");
        System.out.println(decryptedText);
    }


    public static String decrypt(String encryptedText) {
        String decryptedText = null;
        try {
            byte[] key = edKey.getBytes("UTF-8");
            byte[] iv = ivKey.getBytes("UTF-8");
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("Key length must be 16, 24, or 32 bytes.");
            }
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv); // 128-bit authentication tag length
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            decryptedText = new String(decryptedBytes, "UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Decrypt error: " + e.getMessage());
        }
        return decryptedText;
    }

}