package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.College;
import com.smartqueue.app.entity.Department;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;
import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.repository.CounterRepository;
import com.smartqueue.app.service.CollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/college")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @Autowired
    private CounterRepository counterRepository;

    @GetMapping("/branch/{branchId}/counters")
    public ResponseEntity<ApiResponse<List<Counter>>> getCounters(@PathVariable Long branchId) {
        List<Counter> counters = counterRepository.findByTypeAndReferenceId("COLLEGE", branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Counters fetched successfully!", counters));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<College>>> getAllColleges() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Colleges fetched successfully!", collegeService.getAllColleges()));
    }

    @GetMapping("/{collegeId}/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartments(@PathVariable Long collegeId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Departments fetched successfully!", collegeService.getDepartmentsByCollege(collegeId)));
    }

    @GetMapping("/department/{departmentId}/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlots(
            @PathVariable Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", collegeService.getSlotsByDepartmentAndDate(departmentId, date)));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Appointment>> bookAppointment(
            @RequestParam Long userId,
            @RequestParam Long slotId,
            @RequestParam Long departmentId,
            @RequestParam String serviceName) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully!", collegeService.bookAppointment(userId, slotId, departmentId, serviceName)));
    }

    @PostMapping("/appointment/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long appointmentId) {
        collegeService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully!", null));
    }
}
