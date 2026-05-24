package com.smartqueue.app.service;

public interface PasswordResetService {
    void processForgotPassword(String email);
    void processResetPassword(String token, String newPassword);
}
