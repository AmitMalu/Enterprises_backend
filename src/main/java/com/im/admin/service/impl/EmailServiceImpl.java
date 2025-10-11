package com.im.admin.service.impl;

import com.im.admin.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleEmail(String toEmail, String subject, String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        System.out.println(" Mail sent successfully to " + toEmail);
    }

    public String buildWelcomeEmail(String userName, String mobileNo, String password) {
        return "Dear " + userName + ",\n\n" +
                "We’re delighted to welcome you to NFT!\n" +
                "Your account has been successfully created, and you can now log in to your dashboard to start doing business with us.\n\n" +
                "Below are your login credentials:\n\n" +
                "Portal Link: https://uat.decipay.in/enterprise-portal/login:\n\n"  +
                "User ID: " + mobileNo + "\n" +
                "Password: " + password + "\n\n" +
                "For security reasons, we recommend changing your password after your first login.\n\n" +
                "Best Regards,\nNFT Team";
    }
    }

