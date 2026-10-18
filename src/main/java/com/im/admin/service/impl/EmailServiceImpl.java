package com.im.admin.service.impl;

import com.im.admin.entities.EmailCredentialsMaster;
import com.im.admin.entities.EmailLog;
import com.im.admin.entities.EmailTemplateMaster;
import com.im.admin.entities.enums.EmailType;
import com.im.admin.repositories.EmailLogRepository;
import com.im.admin.repositories.EmailTemplateRepository;
import com.im.admin.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    /** Login link included in onboarding emails. */
    @Value("${app.admin.login-url:https://admin.nfttech.in/enterprise-portal/login}")
    private String portalLoginUrl;


    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Override
    @Transactional
    public void sendSimpleEmail(EmailCredentialsMaster credentials,String fromEmail, String fromPassword, String toEmail, String subject, String body) {

        LocalDateTime requestTime = LocalDateTime.now();
        String request = "To: " + toEmail + "\nSubject: " + subject + "\nBody: " + body;
        String response = "";

        try {

            JavaMailSenderImpl dynamicSender = new JavaMailSenderImpl();

            dynamicSender.setHost("mail.networkfuturetech.co.in");
            dynamicSender.setPort(465);

            dynamicSender.setUsername(fromEmail);
            dynamicSender.setPassword(fromPassword);

            Properties props = dynamicSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.trust", "mail.networkfuturetech.co.in");
            props.put("mail.debug", "true");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            dynamicSender.send(message);

            response = "Mail sent successfully to " + toEmail;

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save log
        EmailLog emailLog = new EmailLog();
        emailLog.setRequest(request);
        emailLog.setResponse(response);
        emailLog.setRequestDateTime(requestTime);
        emailLog.setResponseDateTime(LocalDateTime.now());
        emailLog.setIpAddress(null);

        emailLogRepository.save(emailLog);

    }

    @Override
    public String buildWelcomeEmail(String userName, String mobileNo, String password) {
        return "Dear " + userName + ",\n\n" +
                "We’re delighted to welcome you to Smart Credit Solution!\n" +
                "Your account has been successfully created, and you can now log in to your dashboard to start doing business with us.\n\n" +
                "Below are your login credentials:\n\n" +
                "Portal Link: " + portalLoginUrl + "\n\n" +
                "User ID: " + mobileNo + "\n" +
                "Password: " + password + "\n\n" +
                "For security reasons, we recommend changing your password after your first login.\n\n" +
                "Best Regards,\nSmart Credit Solution Team";
    }

    public String buildEmailFromDB(EmailType type, Map<String, String> variables) {

        EmailTemplateMaster template = emailTemplateRepository
                .findByType(type.name())
                .orElseThrow(() -> new RuntimeException("Template not found"));

        String body = template.getTemplate();

        // Replace variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return body;
    }
}

