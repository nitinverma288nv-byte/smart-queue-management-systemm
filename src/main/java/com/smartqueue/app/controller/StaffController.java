package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.Staff;
import com.smartqueue.app.entity.Token;
import com.smartqueue.app.entity.User;
import com.smartqueue.app.repository.UserRepository;
import com.smartqueue.app.service.StaffService;
import com.smartqueue.app.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176", "http://localhost:5177", "http://localhost:5178", "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175", "http://127.0.0.1:5176", "http://127.0.0.1:5177", "http://127.0.0.1:5178"})
public class StaffController {

    @Autowired
    private StaffService staffService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResponse<Staff>> getStaffProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff profile fetched successfully!", staffService.getStaffByUserId(userId)));
    }

    @GetMapping("/queue/serving/{userId}")
    public ResponseEntity<ApiResponse<Token>> getCurrentlyServingByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Currently serving token fetched!", staffService.getCurrentlyServingTokenByUserId(userId)));
    }

    @GetMapping("/queue/serving")
    public ResponseEntity<ApiResponse<Token>> getCurrentlyServingByUserQuery(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            userId = resolveAuthenticatedUserId();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Currently serving token fetched!", staffService.getCurrentlyServingTokenByUserId(userId)));
    }

    @GetMapping("/queue/waiting/{userId}")
    public ResponseEntity<ApiResponse<List<Token>>> getWaitingByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Waiting tokens fetched!", staffService.getWaitingTokensByUserId(userId)));
    }

    @GetMapping("/queue/waiting")
    public ResponseEntity<ApiResponse<List<Token>>> getWaitingByUserQuery(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            userId = resolveAuthenticatedUserId();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Waiting tokens fetched!", staffService.getWaitingTokensByUserId(userId)));
    }

    private Long resolveAuthenticatedUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return user.getId();
    }

    @PostMapping("/queue/call-next")
    public ResponseEntity<ApiResponse<Token>> callNext(
            @RequestParam(required = false) Long counterId) {
        if (counterId == null) {
            Long userId = resolveAuthenticatedUserId();
            Staff staff = staffService.getStaffByUserId(userId);
            if (staff.getCounter() == null) {
                throw new RuntimeException("Assigned counter not found for staff user ID: " + userId);
            }
            counterId = staff.getCounter().getId();
        }
        Token token = tokenService.callNextToken(counterId);
        return ResponseEntity.ok(new ApiResponse<>(true, token != null ? "Called next token: " + token.getTokenNumber() : "No tokens waiting in queue!", token));
    }

    @PostMapping("/queue/call-specific")
    public ResponseEntity<ApiResponse<Token>> callSpecific(
            @RequestParam Long tokenId,
            @RequestParam(required = false) Long counterId) {
        if (counterId == null) {
            Long userId = resolveAuthenticatedUserId();
            Staff staff = staffService.getStaffByUserId(userId);
            if (staff.getCounter() == null) {
                throw new RuntimeException("Assigned counter not found for staff user ID: " + userId);
            }
            counterId = staff.getCounter().getId();
        }
        Token token = tokenService.callSpecificToken(tokenId, counterId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Called specific token: " + token.getTokenNumber(), token));
    }

    @Autowired
    private com.smartqueue.app.repository.CounterRepository counterRepository;

    @Autowired
    private com.smartqueue.app.repository.StaffRepository staffRepository;

    @GetMapping("/counters")
    public ResponseEntity<ApiResponse<List<com.smartqueue.app.entity.Counter>>> getCounters(
            @RequestParam String sectorType,
            @RequestParam Long branchId) {
        List<com.smartqueue.app.entity.Counter> counters = counterRepository.findByTypeAndReferenceId(sectorType.toUpperCase(), branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Counters fetched!", counters));
    }

    @PostMapping("/update-counter")
    public ResponseEntity<ApiResponse<Staff>> updateCounter(
            @RequestParam String sectorType,
            @RequestParam Long referenceId,
            @RequestParam Long counterId) {
        Long userId = resolveAuthenticatedUserId();
        Staff staff = staffService.getStaffByUserId(userId);
        
        if (staff.getUser().getStaffType() != null && !staff.getUser().getStaffType().equalsIgnoreCase(sectorType)) {
            throw new com.smartqueue.app.exception.BadRequestException("Unauthorized cross-sector desk switch! You are assigned to " + staff.getUser().getStaffType());
        }

        if (staff.getReferenceId() != null && !staff.getReferenceId().equals(referenceId)) {
            throw new com.smartqueue.app.exception.BadRequestException("Unauthorized cross-branch desk switch! You are assigned to branch/campus ID: " + staff.getReferenceId());
        }

        com.smartqueue.app.entity.Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new com.smartqueue.app.exception.ResourceNotFoundException("Counter not found!"));

        if (!counter.getBranchId().equals(referenceId)) {
            throw new com.smartqueue.app.exception.BadRequestException("Counter does not belong to your assigned branch/campus!");
        }

        staff.setSectorType(sectorType.toUpperCase());
        staff.setReferenceId(referenceId);
        staff.setCounter(counter);

        Staff updatedStaff = staffRepository.save(staff);

        // Update counter's assigned staff ID
        counter.setAssignedStaffId(updatedStaff.getId());
        counterRepository.save(counter);

        return ResponseEntity.ok(new ApiResponse<>(true, "Counter updated successfully!", updatedStaff));
    }

    @PostMapping("/token/{tokenId}/skip")
    public ResponseEntity<ApiResponse<Token>> skipToken(@PathVariable Long tokenId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Token marked as skipped!", tokenService.skipToken(tokenId)));
    }

    @PostMapping("/token/{tokenId}/complete")
    public ResponseEntity<ApiResponse<Token>> completeToken(@PathVariable Long tokenId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Token marked as completed!", tokenService.completeToken(tokenId)));
    }
}
