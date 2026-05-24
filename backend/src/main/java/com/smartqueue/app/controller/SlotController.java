package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Counter;
import com.smartqueue.app.repository.SlotRepository;
import com.smartqueue.app.repository.CounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class SlotController {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private CounterRepository counterRepository;

    @GetMapping("/by-counter/{counterId}")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlotsByCounter(
            @PathVariable Long counterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Slot> slots;
        if (date != null) {
            slots = slotRepository.findByCounterIdAndDateAndAvailabilityTrue(counterId, date);
            if (slots.isEmpty()) {
                Counter counter = counterRepository.findById(counterId).orElse(null);
                if (counter != null) {
                    String type = counter.getSectorType();
                    slotRepository.save(Slot.builder().type(type).referenceId(counterId).sectorType(type).branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                    slotRepository.save(Slot.builder().type(type).referenceId(counterId).sectorType(type).branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                    slotRepository.save(Slot.builder().type(type).referenceId(counterId).sectorType(type).branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                    slotRepository.save(Slot.builder().type(type).referenceId(counterId).sectorType(type).branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                    slotRepository.save(Slot.builder().type(type).referenceId(counterId).sectorType(type).branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                    slots = slotRepository.findByCounterIdAndDateAndAvailabilityTrue(counterId, date);
                }
            }
        } else {
            slots = slotRepository.findByCounterIdAndAvailabilityTrue(counterId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", slots));
    }
}
