package com.im.admin.service;

import com.im.admin.entities.EmailCredentialsMaster;
import com.im.admin.entities.enums.EmailType;

import java.util.Map;

public interface EmailService {

    void sendSimpleEmail(EmailCredentialsMaster credentials,String from, String to, String fromPassword, String subject, String body);

    String buildWelcomeEmail(String userName, String mobileNo, String password);

    String buildEmailFromDB(EmailType emailType, Map<String, String> vars);
}
