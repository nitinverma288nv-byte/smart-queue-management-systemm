package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Hospital;
import com.smartqueue.app.entity.HospitalBranch;
import com.smartqueue.app.entity.Doctor;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;
import com.smartqueue.app.repository.DoctorRepository;
import com.smartqueue.app.repository.SlotRepository;
import com.smartqueue.app.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/hospital")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SlotRepository slotRepository;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Hospital>>> getAllHospitals() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Hospitals fetched successfully!", hospitalService.getAllHospitals()));
    }

    @GetMapping("/{hospitalId}/branches")
    public ResponseEntity<ApiResponse<List<HospitalBranch>>> getBranches(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branches fetched successfully!", hospitalService.getBranchesByHospital(hospitalId)));
    }

    @GetMapping("/{hospitalId}/doctors")
    public ResponseEntity<ApiResponse<List<Doctor>>> getDoctorsByHospital(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors fetched successfully!", hospitalService.getDoctorsByHospital(hospitalId)));
    }

    @GetMapping("/branch/{branchId}/doctors")
    public ResponseEntity<ApiResponse<List<Doctor>>> getDoctors(@PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Doctors fetched successfully!", hospitalService.getDoctorsByBranch(branchId)));
    }

    @GetMapping("/doctor/{doctorId}/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = hospitalService.getSlotsByDoctorAndDate(doctorId, date);
        if (slots.isEmpty()) {
            Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
            if (doctor != null) {
                slotRepository.save(Slot.builder().type("DOCTOR").referenceId(doctorId).sectorType("HOSPITAL").branchId(doctor.getBranch().getId()).date(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("DOCTOR").referenceId(doctorId).sectorType("HOSPITAL").branchId(doctor.getBranch().getId()).date(date).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("DOCTOR").referenceId(doctorId).sectorType("HOSPITAL").branchId(doctor.getBranch().getId()).date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("DOCTOR").referenceId(doctorId).sectorType("HOSPITAL").branchId(doctor.getBranch().getId()).date(date).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("DOCTOR").referenceId(doctorId).sectorType("HOSPITAL").branchId(doctor.getBranch().getId()).date(date).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slots = hospitalService.getSlotsByDoctorAndDate(doctorId, date);
            }
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", slots));
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
