package com.smartqueue.app.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String token, String fullName);
    void sendAppointmentTimingEmail(String toEmail, String fullName, String serviceName, String timing, String sector);
    void sendTokenBookingEmail(String toEmail, String fullName, String tokenNumber, String sectorType, String serviceName, String timing, String priority, String location);
}
