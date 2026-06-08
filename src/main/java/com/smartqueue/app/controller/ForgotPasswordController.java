package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class ForgotPasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Email address is required!", null));
        }
        passwordResetService.processForgotPassword(email.trim());
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset email sent successfully! Please check your inbox.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Reset token is required!", null));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "New password is required!", null));
        }
        passwordResetService.processResetPassword(token.trim(), password);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password has been reset successfully! You can now log in.", null));
    }
}
