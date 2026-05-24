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
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class AdminController {

    @Autowired
    private AdminService adminService;

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
    private CounterRepository counterRepository;

    @Autowired
    private SlotRepository slotRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Stats compiled successfully!", adminService.getDashboardStats()));
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
}
