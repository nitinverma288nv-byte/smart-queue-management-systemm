package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Hospital;
import com.smartqueue.app.entity.HospitalBranch;
import com.smartqueue.app.entity.Doctor;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;
import com.smartqueue.app.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hospital")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Hospital>>> getAllHospitals() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Hospitals fetched successfully!", hospitalService.getAllHospitals()));
    }

    @GetMapping("/{hospitalId}/branches")
    public ResponseEntity<ApiResponse<List<HospitalBranch>>> getBranches(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branches fetched successfully!", hospitalService.getBranchesByHospital(hospitalId)));
    }

    @GetMapping("/branch/{branchId}/doctors")
    public ResponseEntity<ApiResponse<List<Doctor>>> getDoctors(@PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors fetched successfully!", hospitalService.getDoctorsByBranch(branchId)));
    }

    @GetMapping("/doctor/{doctorId}/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", hospitalService.getSlotsByDoctorAndDate(doctorId, date)));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Appointment>> bookAppointment(
            @RequestParam Long userId,
            @RequestParam Long slotId,
            @RequestParam Long doctorId,
            @RequestParam String serviceName) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully!", hospitalService.bookAppointment(userId, slotId, doctorId, serviceName)));
    }

    @PostMapping("/appointment/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long appointmentId) {
        hospitalService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully!", null));
    }
}
