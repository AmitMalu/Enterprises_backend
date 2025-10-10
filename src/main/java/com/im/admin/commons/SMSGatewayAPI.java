package com.im.admin.commons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Service
public class SMSGatewayAPI {

    private CommonFunctions commonFunctions;

    public String sendOTPSms(String mobileNumber, String otp) {
        try {
            // Build request parameters
            String requestMessage = "Your One Time Password is " + otp + " -Instant Mudra.";
            String encodedMsg = URLEncoder.encode(requestMessage, StandardCharsets.UTF_8);

            String data = "send_to=" + mobileNumber + "&" +
                    "method=sendMessage&" +
                    "msg=" + encodedMsg + "&" +
                    "msg_type=TEXT&auth_scheme=plain&v=1.1&format=text";

            String fullUrl = "https://enterprise.smsgupshup.com/GatewayAPI/rest?userid=2000197160&password=IMudra@2022&" + data;

            LocalDateTime requestDateTime = LocalDateTime.now();

            String requestLog = fullUrl;

            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.connect();

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuffer = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                responseBuffer.append(line).append("\n");
            }
            rd.close();
            conn.disconnect();

            String response = responseBuffer.toString().trim();

            LocalDateTime responseDateTime = LocalDateTime.now();

            commonFunctions.saveLoginLogs(
                    requestLog,        // JSON request string
                    requestDateTime,   // Request timestamp
                    response,          // JSON response string
                    responseDateTime,  // Response timestamp
                    null               // IP address (optional)
            );


            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "FAILED";
        }
    }

}
