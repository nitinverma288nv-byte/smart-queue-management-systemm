package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.*;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private SlotRepository slotRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Stats compiled successfully!", adminService.getDashboardStats()));
    }

    @GetMapping("/tokens")
    public ResponseEntity<ApiResponse<List<Token>>> getAllTokens() {
        return ResponseEntity.ok(new ApiResponse<>(true, "All tokens fetched!", tokenRepository.findAll()));
    }

    @GetMapping("/hospitals")
    public ResponseEntity<ApiResponse<List<Hospital>>> getHospitals() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Hospitals fetched!", hospitalRepository.findAll()));
    }

    @PostMapping("/hospital")
    public ResponseEntity<ApiResponse<Hospital>> createHospital(@RequestBody Hospital hospital) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Hospital created!", hospitalRepository.save(hospital)));
    }

    @DeleteMapping("/hospital/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHospital(@PathVariable Long id) {
        hospitalRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Hospital deleted!", null));
    }

    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<Bank>>> getBanks() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Banks fetched!", bankRepository.findAll()));
    }

    @PostMapping("/bank")
    public ResponseEntity<ApiResponse<Bank>> createBank(@RequestBody Bank bank) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank created!", bankRepository.save(bank)));
    }

    @DeleteMapping("/bank/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBank(@PathVariable Long id) {
        bankRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bank deleted!", null));
    }

    @GetMapping("/colleges")
    public ResponseEntity<ApiResponse<List<College>>> getColleges() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Colleges fetched!", collegeRepository.findAll()));
    }

    @PostMapping("/college")
    public ResponseEntity<ApiResponse<College>> createCollege(@RequestBody College college) {
        return ResponseEntity.ok(new ApiResponse<>(true, "College created!", collegeRepository.save(college)));
    }

    @DeleteMapping("/college/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCollege(@PathVariable Long id) {
        collegeRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "College deleted!", null));
    }

    @PostMapping("/doctor")
    public ResponseEntity<ApiResponse<Doctor>> createDoctor(@RequestBody Doctor doctor) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctor created!", doctorRepository.save(doctor)));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<Staff>> createStaff(@RequestBody Staff staff) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff created!", staffRepository.save(staff)));
    }

    @PostMapping("/counter")
    public ResponseEntity<ApiResponse<Counter>> createCounter(@RequestBody Counter counter) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Counter created!", counterRepository.save(counter)));
    }

    @PostMapping("/slot")
    public ResponseEntity<ApiResponse<Slot>> createSlot(@RequestBody Slot slot) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Slot created!", slotRepository.save(slot)));
    }

    private void checkAdminAccess() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.smartqueue.app.exception.ResourceNotFoundException("User not found!"));
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new com.smartqueue.app.exception.BadRequestException("Access Denied: Only Admin can perform this action.");
        }
    }

    @GetMapping("/users-list")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsersList() {
        checkAdminAccess();
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_USER)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Users list fetched successfully!", users));
    }

    @GetMapping("/staff-list")
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaffList() {
        checkAdminAccess();
        List<Staff> staff = staffRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff list fetched successfully!", staff));
    }

    @PostMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<User>> toggleUserStatus(@PathVariable Long id) {
        checkAdminAccess();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.smartqueue.app.exception.ResourceNotFoundException("User not found!"));
        user.setStatus("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        return ResponseEntity.ok(new ApiResponse<>(true, "User status toggled!", userRepository.save(user)));
    }

    @PostMapping("/staff/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Staff>> toggleStaffStatus(@PathVariable Long id) {
        checkAdminAccess();
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new com.smartqueue.app.exception.ResourceNotFoundException("Staff not found!"));
        User user = staff.getUser();
        user.setStatus("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff status toggled!", staff));
    }
}
