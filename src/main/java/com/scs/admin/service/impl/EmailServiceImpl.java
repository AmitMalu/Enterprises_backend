package com.scs.admin.service.impl;

import com.scs.admin.entities.EmailCredentialsMaster;
import com.scs.admin.entities.EmailLog;
import com.scs.admin.entities.EmailTemplateMaster;
import com.scs.admin.entities.enums.EmailType;
import com.scs.admin.repositories.EmailLogRepository;
import com.scs.admin.repositories.EmailTemplateRepository;
import com.scs.admin.service.EmailService;
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
    @Value("${app.admin.login-url:https://admin1.utsabpay.com/enterprise-portal/login}")
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

            // SMTP server comes from Email Credentials (admin portal); port 465 = implicit SSL
            String smtpHost = credentials != null ? credentials.getSmtpHost() : null;
            if (smtpHost == null || smtpHost.isBlank()) {
                throw new IllegalStateException("SMTP host is not set in Email Credentials");
            }
            int smtpPort = credentials.getSmtpPort() != null ? credentials.getSmtpPort() : 465;
            boolean implicitSsl = smtpPort == 465;
            dynamicSender.setHost(smtpHost);
            dynamicSender.setPort(smtpPort);

            dynamicSender.setUsername(fromEmail);
            dynamicSender.setPassword(fromPassword);

            Properties props = dynamicSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", String.valueOf(implicitSsl));
            props.put("mail.smtp.starttls.enable", String.valueOf(!implicitSsl));
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            dynamicSender.send(message);

            response = "Mail sent successfully to " + toEmail;

        } catch (Exception e) {
            response = "Mail failed: " + e.getMessage();
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

