package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.ResetToken;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.ResetTokenRepository;
import com.smartqueue.app.repository.UserRepository;
import com.smartqueue.app.service.EmailService;
import com.smartqueue.app.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user registered with this email address!"));

        // Delete pre-existing token if any exists for the user
        resetTokenRepository.findByUser(user).ifPresent(t -> resetTokenRepository.delete(t));

        // Generate token and expiry (24 hours from now)
        String token = UUID.randomUUID().toString();
        ResetToken resetToken = ResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        resetTokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getFullName());
    }

    @Override
    public void processResetPassword(String token, String newPassword) {
        ResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset link!"));

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new BadRequestException("This password reset link has expired!");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Terminate reset token
        resetTokenRepository.delete(resetToken);
    }
}
