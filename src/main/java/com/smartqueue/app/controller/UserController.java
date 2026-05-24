package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getPhoneNumber(),
                user.getProfileImage()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Profile loaded successfully!", profile));
    }

    @PutMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                throw new BadRequestException("Email is already registered by another user!");
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        user.setPhoneNumber(request.getPhoneNumber());
        user.setProfileImage(request.getProfileImage());

        User updatedUser = userRepository.save(user);

        UserProfileResponse profile = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getFullName(),
                updatedUser.getRole().name(),
                updatedUser.getPhoneNumber(),
                updatedUser.getProfileImage()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully!", profile));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody PasswordChangeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            throw new BadRequestException("Current password is required!");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new BadRequestException("New password is required!");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully!", null));
    }

    // Response and Request DTOs
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private String phoneNumber;
        private String profileImage;

        public UserProfileResponse() {}

        public UserProfileResponse(Long id, String username, String email, String fullName, String role, String phoneNumber, String profileImage) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.phoneNumber = phoneNumber;
            this.profileImage = profileImage;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getProfileImage() { return profileImage; }

        public void setId(Long id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setEmail(String email) { this.email = email; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setRole(String role) { this.role = role; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    }

    public static class ProfileUpdateRequest {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String profileImage;

        public ProfileUpdateRequest() {}

        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getProfileImage() { return profileImage; }

        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setEmail(String email) { this.email = email; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    }

    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;

        public PasswordChangeRequest() {}

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }

        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
