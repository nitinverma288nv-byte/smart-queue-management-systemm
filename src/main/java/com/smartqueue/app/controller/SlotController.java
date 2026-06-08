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
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", 
"http://localhost:5175", "http://localhost:5176", "http://localhost:5177", 
"http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", 
"http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class SlotController {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private CounterRepository counterRepository;

    @GetMapping("/by-counter/{counterId}")
    public ResponseEntity<ApiResponse<List<Slot>>> getSlotsByCounter(
            @PathVariable Long counterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "") String serviceName) {
        
        int duration = 30; // default 30 mins
        int maxCap = 3; // default capacity 3
        String s = serviceName.toLowerCase();

        Counter counterOpt = counterRepository.findById(counterId).orElse(null);
        String type = counterOpt != null ? counterOpt.getSectorType() : "COLLEGE";

        if ("HOSPITAL".equalsIgnoreCase(type)) {
            if (s.contains("cold") || s.contains("fever") || s.contains("allergy") || s.contains("skin") || 
                s.contains("ent") || s.contains("diabet") || s.contains("eye") || s.contains("checkup") || s.contains("consult")) {
                duration = 15;
            } else if (s.contains("headache")) {
                duration = 20;
            } else if (s.contains("dental") || s.contains("ortho")) {
                duration = 30;
            } else if (s.contains("cardio")) {
                duration = 45;
            } else if (s.contains("neuro")) {
                duration = 60;
            } else if (s.contains("emergency")) {
                duration = 10;
            }
        } else if ("BANK".equalsIgnoreCase(type)) {
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
        } else if ("COLLEGE".equalsIgnoreCase(type)) {
            if (s.contains("fees") || s.contains("exam")) {
                duration = 15;
            } else if (s.contains("scholarship")) {
                duration = 30;
            } else if (s.contains("bonafide") || s.contains("card") || s.contains("id")) {
                duration = 10;
            }
        }

        List<Slot> slots;
        if (date != null) {
            slots = slotRepository.findByCounterIdAndDateAndAvailabilityTrue(counterId, date);
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
                    if (s.contains("counselling") || s.contains("locker") || s.contains("vault")) {
                        maxCap = 2; // special capacity
                        // 09:00 AM - 01:00 PM (4 hours)
                        slotRepository.save(Slot.builder()
                                .type(type).referenceId(counterId).sectorType(type)
                                .branchId(counter.getBranchId()).counterId(counterId).date(date)
                                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(13, 0))
                                .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                                .build());
                        // 01:30 PM - 04:30 PM (3 hours)
                        slotRepository.save(Slot.builder()
                                .type(type).referenceId(counterId).sectorType(type)
                                .branchId(counter.getBranchId()).counterId(counterId).date(date)
                                .startTime(LocalTime.of(13, 30)).endTime(LocalTime.of(16, 30))
                                .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                                .build());
                        // 05:00 PM - 08:00 PM (3 hours)
                        slotRepository.save(Slot.builder()
                                .type(type).referenceId(counterId).sectorType(type)
                                .branchId(counter.getBranchId()).counterId(counterId).date(date)
                                .startTime(LocalTime.of(17, 0)).endTime(LocalTime.of(20, 0))
                                .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                                .build());
                    } else {
                        LocalTime time = LocalTime.of(9, 0);
                        LocalTime endTimeOfDay = LocalTime.of(16, 0);
                        
                        if ("HOSPITAL".equalsIgnoreCase(type)) {
                            time = LocalTime.of(9, 0);
                            endTimeOfDay = LocalTime.of(17, 0); // 9 AM to 5 PM
                        } else if ("BANK".equalsIgnoreCase(type)) {
                            time = LocalTime.of(10, 0);
                            endTimeOfDay = LocalTime.of(16, 0); // 10 AM to 4 PM
                        } else if ("COLLEGE".equalsIgnoreCase(type)) {
                            time = LocalTime.of(10, 0);
                            endTimeOfDay = LocalTime.of(15, 0); // 10 AM to 3 PM
                        }
                        while (time.isBefore(endTimeOfDay)) {
                            LocalTime nextTime = time.plusMinutes(duration);
                            if (nextTime.isAfter(endTimeOfDay)) break;
                            
                            slotRepository.save(Slot.builder()
                                    .type(type).referenceId(counterId).sectorType(type)
                                    .branchId(counter.getBranchId()).counterId(counterId).date(date)
                                    .startTime(time).endTime(nextTime)
                                    .maxTokens(maxCap).bookedTokens(0).availability(true).maxCapacity(maxCap)
                                    .build());
                            time = nextTime;
                        }
                    }
                    slots = slotRepository.findByCounterIdAndDateAndAvailabilityTrue(counterId, date);
                }
            }
        } else {
            slots = slotRepository.findByCounterIdAndAvailabilityTrue(counterId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Slots fetched successfully!", slots));
    }
}
