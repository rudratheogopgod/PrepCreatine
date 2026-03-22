package com.prepcreatine.service;

import com.prepcreatine.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional emails via SMTP.
 * Uses @Async to avoid blocking request threads.
 * HTML templates are built inline — for production,
 * use a template engine (Thymeleaf) per BSDD §8 email spec.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties  app;

    public EmailService(JavaMailSender mailSender, AppProperties app) {
        this.mailSender = mailSender;
        this.app        = app;
    }

    @Async
    public void sendVerificationEmail(String to, String name, String token) {
        String link    = app.getFrontendUrl() + "/verify-email?token=" + token;
        String subject = "Verify your PrepCreatine email";
        String html    = buildEmailHtml(name,
            "Please verify your email address to start using PrepCreatine.",
            "Verify Email", link);

        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        String link    = app.getFrontendUrl() + "/reset-password?token=" + token;
        String subject = "Reset your PrepCreatine password";
        String html    = buildEmailHtml(name,
            "We received a request to reset your password. This link expires in 1 hour.",
            "Reset Password", link);

        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendMentorConnectedEmail(String to, String studentName, String mentorName) {
        String subject = "You're now connected with " + mentorName;
        String html    = buildEmailHtml(studentName,
            "Your mentor <strong>" + mentorName + "</strong> can now view your study progress.",
            "View Dashboard", app.getFrontendUrl() + "/dashboard");

        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendStreakMilestoneEmail(String to, String name, int streak) {
        String subject = "🔥 " + streak + "-day streak milestone!";
        String html    = buildEmailHtml(name,
            "Incredible! You've maintained a " + streak + "-day study streak on PrepCreatine. Keep going!",
            "Continue Studying", app.getFrontendUrl() + "/dashboard");

        sendHtmlEmail(to, subject, html);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("PrepCreatine <" + app.getEmailFrom() + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[Email] Sent '{}' to {}", subject, to);
        } catch (MessagingException e) {
            log.error("[Email] Failed to send '{}' to {}: {}", subject, to, e.getMessage());
            // Do NOT rethrow — email failure should not break auth flows
        }
    }

    private String buildEmailHtml(String name, String bodyText, String ctaLabel, String ctaUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; margin:0; padding: 20px;">
              <div style="max-width:480px; margin:0 auto; background:#fff; border-radius:12px; padding:32px; box-shadow:0 1px 4px rgba(0,0,0,.08);">
                <h2 style="color:#7C3AED; margin-top:0;">PrepCreatine</h2>
                <p style="color:#111;">Hi %s,</p>
                <p style="color:#555; line-height:1.6;">%s</p>
                <a href="%s" style="display:inline-block; margin-top:16px; padding:12px 28px; background:#7C3AED; color:#fff; border-radius:8px; text-decoration:none; font-weight:600;">%s</a>
                <p style="color:#aaa; font-size:12px; margin-top:32px;">This email was sent by PrepCreatine. If you didn't request this, please ignore it.</p>
              </div>
            </body>
            </html>
            """.formatted(name, bodyText, ctaUrl, ctaLabel);
    }
}
