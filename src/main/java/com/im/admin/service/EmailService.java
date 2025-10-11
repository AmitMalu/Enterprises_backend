package com.im.admin.service;

public interface EmailService {
    void sendSimpleEmail(String to, String subject, String body);

    String buildWelcomeEmail(String userName, String mobileNo, String password);
}
