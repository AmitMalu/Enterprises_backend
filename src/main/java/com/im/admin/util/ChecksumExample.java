package com.im.admin.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class ChecksumExample {

    public static String generateChecksum(String data, String apiKey) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA512");

        SecretKeySpec secretKey = new SecretKeySpec(
                apiKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
        );

        mac.init(secretKey);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) hex.append('0');
            hex.append(s);
        }

        return hex.toString().toUpperCase();
    }

    public static void main(String[] args) {
        try {

            String data = "<NTDREQ><REQTYPE>GPB</REQTYPE></NTDREQ>";
            String apiKey = "2zOShm31VWgnidAvQISxgJjIXXmadlNJac3";

            String checksum = generateChecksum(data, apiKey);

            System.out.println("Checksum = " + checksum);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}