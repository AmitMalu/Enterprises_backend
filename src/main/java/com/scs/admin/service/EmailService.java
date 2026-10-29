package com.scs.admin.service;

import com.scs.admin.entities.EmailCredentialsMaster;
import com.scs.admin.entities.enums.EmailType;

import java.util.Map;

public interface EmailService {

    void sendSimpleEmail(EmailCredentialsMaster credentials,String from, String to, String fromPassword, String subject, String body);

    String buildWelcomeEmail(String userName, String mobileNo, String password);

    String buildEmailFromDB(EmailType emailType, Map<String, String> vars);
}
