package com.scs.admin.util;

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

            //beneficiary verify request
            String data = "<NTDREQ><REQTYPE>BAV</REQTYPE><REFNO>USP00038747843</REFNO><CSTNO>7506821018</CSTNO><CSTNM>Shubham Khopade</CSTNM><BANKID>100</BANKID><ACCNO>38371142066</ACCNO><IFSC>SBIN0000001</IFSC></NTDREQ>";
            String apiKey = "l944exhoFiQNRht0Z4eDHdWsFnSouI4dOSH";

            String checksum = generateChecksum(data, apiKey);

            System.out.println("Checksum = " + checksum);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}