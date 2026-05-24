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
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots = bankService.getSlotsByCounterAndDate(counterId, date);
        if (slots.isEmpty()) {
            Counter counter = counterRepository.findById(counterId).orElse(null);
            if (counter != null) {
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
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
