package com.im.admin.commons;

import com.im.admin.entities.SmsCredentialsMaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import java.time.LocalDateTime;

@Service
public class ShivSmsService {

    @Value("${sms.shiv.base-url}")
    private String baseUrl;

    @Value("${sms.shiv.user}")
    private String user;

    @Value("${sms.shiv.password}")
    private String password;

    @Value("${sms.shiv.sender-id}")
    private String senderId;

    @Value("${sms.shiv.pe-id}")
    private String peId;

    @Value("${sms.shiv.template-id}")
    private String templateId;

    @Value("${sms.shiv.route}")
    private String route;

    @Autowired
    private CommonFunctions commonFunctions;

//    public String sendOtpSms(String mobileNumber, String name, String otp) {
//        try {
//
//            // EXACT TEMPLATE STYLE (like your working URL)
//            String message = "Dear " + name
//                    + ", please enter the given otp to access your account, it will be expire in 2 min. OTP: "
//                    + otp
//                    + " Please do not share your Otp with anyone for security reason. Thank you for choosing NFT NETWORK TECHNOLOGIES PRIVATE LIMITED.";
//
//            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
//
//            String url = baseUrl
//                    + "?user=" + user
//                    + "&password=" + password
//                    + "&senderid=" + senderId
//                    + "&channel=Trans"
//                    + "&DCS=0"
//                    + "&flashsms=0"
//                    + "&number=91" + mobileNumber
//                    + "&text=" + encodedMessage
//                    + "&route=" + route
//                    + "&Peid=" + peId
//                    + "&DLTTemplateId=" + templateId;
//
//            LocalDateTime requestTime = LocalDateTime.now();
//
//            URL obj = new URL(url);
//            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setConnectTimeout(10000);
//            conn.setReadTimeout(10000);
//
//            BufferedReader in = new BufferedReader(
//                    new InputStreamReader(conn.getInputStream())
//            );
//
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//
//            String responseStr = response.toString();
//
//            //  LOGGING
//            commonFunctions.saveSmsLogs(
//                    requestTime,
//                    url,
//                    LocalDateTime.now(),
//                    responseStr,
//                    null
//            );
//
//            return responseStr;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            commonFunctions.saveSmsLogs(
//                    LocalDateTime.now(),
//                    "SMS_FAILED",
//                    LocalDateTime.now(),
//                    e.getMessage(),
//                    null
//            );
//
//            return "FAILED";
//        }
//    }

    public String sendOtpSms(SmsCredentialsMaster config, String mobileNumber, String name, String otp,String companyName ) {
        try {

            // 1. Build message dynamically from DB template
//            String companyName = config.getTemplate(); // only company name from DB

            String message = "Dear " + name
                    + ", please enter the given otp to access your account, it will be expire in 2 min. OTP: "
                    + otp
                    + " Please do not share your Otp with anyone for security reason. Thank you for choosing "
                    + companyName;

            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            // 2. Build URL dynamically
            String url = config.getBaseUrl()
                    + "?user=" + config.getUsername()
                    + "&password=" + config.getPassword()
                    + "&senderid=" + config.getSenderId()
                    + "&channel=Trans"
                    + "&DCS=0"
                    + "&flashsms=0"
                    + "&number=91" + mobileNumber
                    + "&text=" + encodedMessage
                    + "&route=" + config.getRoute()
                    + "&Peid=" + config.getPeId()
                    + "&DLTTemplateId=" + config.getDltTemplateId();

            LocalDateTime requestTime = LocalDateTime.now();

            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseStr = response.toString();

            // logging
            commonFunctions.saveSmsLogs(
                    requestTime,
                    url,
                    LocalDateTime.now(),
                    responseStr,
                    null
            );

            return responseStr;

        } catch (Exception e) {
            e.printStackTrace();

            commonFunctions.saveSmsLogs(
                    LocalDateTime.now(),
                    "SMS_FAILED",
                    LocalDateTime.now(),
                    e.getMessage(),
                    null
            );

            return "FAILED";
        }
    }

}
