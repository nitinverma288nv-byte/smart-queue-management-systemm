package com.smartqueue.app.service;

import com.smartqueue.app.dto.AuthRequest;
import com.smartqueue.app.dto.AuthResponse;
import com.smartqueue.app.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
}
