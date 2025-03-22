package com.meetruly.core.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.url:http://localhost:8080}")
    private String baseUrl;

    // Изпращане на обикновен имейл
    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    // Изпращане на HTML имейл със съдържание от шаблон
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Логване на грешката
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // Изпращане на имейл за верификация
    @Async
    public void sendVerificationEmail(String to, String token, String username) {
        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("username", username);

        // Добавяне на пълния URL за верификация
        String verificationUrl = baseUrl + "/auth/verify-email/confirm?token=" + token;
        context.setVariable("verificationUrl", verificationUrl);

        sendHtmlEmail(to, "Email Verification", "verification-email", context);
    }

    // Изпращане на имейл за възстановяване на парола
    @Async
    public void sendPasswordResetEmail(String to, String token, String username) {
        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("username", username);

        // Добавяне на пълния URL за нулиране на парола
        String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
        context.setVariable("resetUrl", resetUrl);

        sendHtmlEmail(to, "Password Reset", "password-reset-email", context);
    }
}