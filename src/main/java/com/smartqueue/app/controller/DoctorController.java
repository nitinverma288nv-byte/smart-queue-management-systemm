package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Doctor;
import com.smartqueue.app.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class DoctorController {

    @Autowired
    private HospitalService hospitalService;

    @GetMapping("/by-branch/{branchId}")
    public ResponseEntity<ApiResponse<List<Doctor>>> getDoctorsByBranch(@PathVariable Long branchId) {
        List<Doctor> doctors = hospitalService.getDoctorsByBranch(branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors fetched successfully!", doctors));
    }
}
