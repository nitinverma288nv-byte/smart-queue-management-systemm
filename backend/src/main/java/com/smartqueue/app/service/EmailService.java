package com.smartqueue.app.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String token, String fullName);
}
