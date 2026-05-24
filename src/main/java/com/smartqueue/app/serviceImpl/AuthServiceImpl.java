package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.dto.AuthRequest;
import com.smartqueue.app.dto.AuthResponse;
import com.smartqueue.app.dto.RegisterRequest;
import com.smartqueue.app.entity.Role;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.UserRepository;
import com.smartqueue.app.security.JwtTokenProvider;
import com.smartqueue.app.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Block admin registration from public endpoint
        if (request.getRole() != null) {
            String roleStr = request.getRole().trim().toUpperCase();
            if (roleStr.contains("ADMIN")) {
                throw new BadRequestException("Administrator accounts are managed securely by the system owner.");
            }
            if (roleStr.contains("STAFF")) {
                throw new BadRequestException("Staff accounts can only be created by an administrator.");
            }
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username is already taken!");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email is already registered!");
        }

        // Public registration ONLY creates ROLE_USER accounts
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);

        return new AuthResponse(token, savedUser.getUsername(), savedUser.getRole().name(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getId());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getEmail(), user.getFullName(), user.getId());
    }
}
