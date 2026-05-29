package com.thluxury.notification.service;

import com.thluxury.notification.entity.NotificationLog;
import com.thluxury.notification.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository logRepository;

    @Value("${thluxury.mail.from}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables, String type) {
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process(templateName, context);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        NotificationLog notificationLog = NotificationLog.builder()
                .type(type)
                .recipient(to)
                .subject(subject)
                .content(htmlContent)
                .build();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            
            notificationLog.setStatus("SUCCESS");
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            notificationLog.setStatus("FAILED");
            notificationLog.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to: {}", to, e);
            notificationLog.setStatus("FAILED");
            notificationLog.setErrorMessage(e.getMessage());
        } finally {
            logRepository.save(notificationLog);
        }
    }
}
