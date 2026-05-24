package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendPasswordResetEmail(String toEmail, String token, String fullName) {
        String resetUrl = "http://localhost:5173/reset-password?token=" + token;
        String subject = "Password Reset Request - SMART QUEUE MANAGEMENT SYSTEM";
        String messageText = "Dear " + fullName + ",\n\n"
                + "We received a request to reset your password for the SMART QUEUE MANAGEMENT SYSTEM.\n"
                + "Please click the link below to define a new password:\n"
                + resetUrl + "\n\n"
                + "Note: This link will expire in 24 hours.\n\n"
                + "If you did not request a password reset, please ignore this email.\n\n"
                + "Regards,\n"
                + "SMART QUEUE MANAGEMENT SYSTEM Support Team";

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(messageText);
                message.setFrom("noreply.smartqueue@gmail.com");
                mailSender.send(message);
                System.out.println("✓ Password reset email sent successfully to: " + toEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send real email using SMTP (ignoring to prevent server crash): " + e.getMessage());
                System.out.println("📬 PASSWORD RESET LINK (FALLBACK LOG): " + resetUrl);
            }
        } else {
            System.out.println("ℹ SMTP Email Sender not configured in properties. Fallback reset link logger:");
            System.out.println("📬 PASSWORD RESET LINK (CONSOLE): " + resetUrl);
        }
    }
}
