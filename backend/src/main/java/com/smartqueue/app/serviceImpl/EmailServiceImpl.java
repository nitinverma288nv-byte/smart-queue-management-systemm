package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String token, String fullName) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
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
                message.setFrom(fromEmail);
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

    @Override
    public void sendAppointmentTimingEmail(String toEmail, String fullName, String serviceName, String timing, String sector) {
        String subject = "Appointment Confirmed - " + sector + " - SMART QUEUE MANAGEMENT SYSTEM";
        String messageText = "Dear " + fullName + ",\n\n"
                + "Your appointment for '" + serviceName + "' in the " + sector + " sector has been successfully booked.\n\n"
                + "Expected Timing / Slot: " + timing + "\n\n"
                + "Please ensure you arrive on time. For any queries, you can log in to the application and check your dashboard.\n\n"
                + "Regards,\n"
                + "SMART QUEUE MANAGEMENT SYSTEM Support Team";

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(messageText);
                message.setFrom(fromEmail);
                mailSender.send(message);
                System.out.println("✓ Appointment email sent successfully to: " + toEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send real email using SMTP (ignoring to prevent server crash): " + e.getMessage());
                System.out.println("📬 APPOINTMENT EMAIL (FALLBACK LOG): to " + toEmail + " | Timing: " + timing);
            }
        } else {
            System.out.println("ℹ SMTP Email Sender not configured in properties. Fallback appointment logger:");
            System.out.println("📬 APPOINTMENT EMAIL (CONSOLE): to " + toEmail + " | Timing: " + timing);
        }
    }

    @Override
    public void sendTokenBookingEmail(String toEmail, String fullName, String tokenNumber, String sectorType, String serviceName, String timing, String priority, String location) {
        String subject = "Token Booked successfully: " + tokenNumber + " - SMART QUEUE MANAGEMENT SYSTEM";
        String messageText = "Dear " + fullName + ",\n\n"
                + "Congratulations! Your dynamic virtual queue token has been successfully generated.\n\n"
                + "---------------------------------------------\n"
                + "               VIRTUAL TICKET PASS           \n"
                + "---------------------------------------------\n"
                + "Token Number  : " + tokenNumber + "\n"
                + "Sector/Branch : " + sectorType + " (" + (location != null ? location : "Main Branch") + ")\n"
                + "Service Name  : " + serviceName + "\n"
                + "Queue Priority: " + priority + "\n"
                + "Slot/Timing   : " + (timing != null ? timing : "Direct Entry Queue") + "\n"
                + "---------------------------------------------\n\n"
                + "IMPORTANT GUIDELINES:\n"
                + "1. Please arrive at least 10 minutes prior to your slot.\n"
                + "2. Keep this email or your digital ticket number handy to show counter staff.\n"
                + "3. You can track your real-time live position in the queue on your dashboard.\n\n"
                + "Need assistance? If you have any technical issues or need help, contact Support at " + fromEmail + "\n\n"
                + "Thank you for using the Smart Queue Management System!\n"
                + "Regards,\n"
                + "SMART QUEUE MANAGEMENT SYSTEM Support Team";

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(messageText);
                message.setFrom(fromEmail);
                mailSender.send(message);
                System.out.println("✓ Token booking confirmation email sent successfully to: " + toEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send token booking email using SMTP: " + e.getMessage());
                System.out.println("📬 TOKEN BOOKING EMAIL (FALLBACK LOG): to " + toEmail + " | Token: " + tokenNumber);
            }
        } else {
            System.out.println("ℹ SMTP Email Sender not configured. Fallback token booking logger:");
            System.out.println("📬 TOKEN BOOKING EMAIL (CONSOLE): to " + toEmail + " | Token: " + tokenNumber);
        }
    }
}
