package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Bank;
import com.smartqueue.app.entity.BankBranch;
import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;
import com.smartqueue.app.repository.CounterRepository;
import com.smartqueue.app.repository.SlotRepository;
import com.smartqueue.app.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class BankController {

    @Autowired
    private BankService bankService;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private SlotRepository slotRepository;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Bank>>> getAllBanks() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Banks fetched successfully!", bankService.getAllBanks()));
    }

    @GetMapping("/{bankId}/branches")
    public ResponseEntity<ApiResponse<List<BankBranch>>> getBranches(@PathVariable Long bankId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Branches fetched successfully!", bankService.getBranchesByBank(bankId)));
    }

    @GetMapping("/branch/{branchId}/counters")
    public ResponseEntity<ApiResponse<List<Counter>>> getCounters(@PathVariable Long branchId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Counters fetched successfully!", bankService.getCountersByBranch(branchId)));
    }

    @GetMapping("/counter/{counterId}/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlots(
            @PathVariable Long counterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "") String serviceName) {
        
        String s = serviceName.toLowerCase();
        int maxCap = 3; // default max capacity 3
        int duration = 30; // default 30 mins
        
        if (s.contains("loan") || s.contains("mortgage")) {
            duration = 45;
        } else if (s.contains("deposit") || s.contains("withdrawal") || s.contains("withdraw")) {
            duration = 15;
        } else if (s.contains("kyc") || s.contains("audit") || s.contains("enquiry")) {
            duration = 30;
        } else if (s.contains("account") || s.contains("open")) {
            duration = 45;
        } else if (s.contains("passbook")) {
            duration = 10;
        }

        List<Slot> slots = bankService.getSlotsByCounterAndDate(counterId, date);
        if (!slots.isEmpty()) {
            boolean hasBookings = slots.stream().anyMatch(slot -> slot.getBookedTokens() > 0);
            if (!hasBookings) {
                slotRepository.deleteAll(slots);
                slots.clear();
            }
        }

        if (slots.isEmpty()) {
            Counter counter = counterRepository.findById(counterId).orElse(null);
            if (counter != null) {
                if (s.contains("locker") || s.contains("vault")) {
                    maxCap = 2; // Vault/Locker max capacity 2
                    // 09:00 AM - 01:00 PM (4 hours)
                    slotRepository.save(Slot.builder()
                            .type("BANK").referenceId(counterId).sectorType("BANK")
                            .branchId(counter.getBranchId()).counterId(counterId).date(date)
                            .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(13, 0))
                            .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                            .build());
                    // 01:30 PM - 04:30 PM (3 hours)
                    slotRepository.save(Slot.builder()
                            .type("BANK").referenceId(counterId).sectorType("BANK")
                            .branchId(counter.getBranchId()).counterId(counterId).date(date)
                            .startTime(LocalTime.of(13, 30)).endTime(LocalTime.of(16, 30))
                            .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                            .build());
                    // 05:00 PM - 08:00 PM (3 hours)
                    slotRepository.save(Slot.builder()
                            .type("BANK").referenceId(counterId).sectorType("BANK")
                            .branchId(counter.getBranchId()).counterId(counterId).date(date)
                            .startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(20, 0))
                            .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                            .build());
                } else {
                    LocalTime time = LocalTime.of(10, 0);
                    LocalTime endTimeOfDay = LocalTime.of(16, 0); // 10 AM to 4 PM
                    while (time.isBefore(endTimeOfDay)) {
                        LocalTime nextTime = time.plusMinutes(duration);
                        if (nextTime.isAfter(endTimeOfDay)) break;
                        
                        slotRepository.save(Slot.builder()
                                .type("BANK").referenceId(counterId).sectorType("BANK")
                                .branchId(counter.getBranchId()).counterId(counterId).date(date)
                                .startTime(time).endTime(nextTime)
                                .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                                .build());
                        time = nextTime;
                    }
                }
                slots = bankService.getSlotsByCounterAndDate(counterId, date);
            }
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", slots));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Appointment>> bookAppointment(
            @RequestParam Long userId,
            @RequestParam Long slotId,
            @RequestParam Long branchId,
            @RequestParam String serviceName) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment booked successfully!", bankService.bookAppointment(userId, slotId, branchId, serviceName)));
    }

    @PostMapping("/appointment/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long appointmentId) {
        bankService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Appointment cancelled successfully!", null));
    }
}
