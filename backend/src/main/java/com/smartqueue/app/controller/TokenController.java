package com.smartqueue.app.controller;

import com.smartqueue.app.dto.ApiResponse;
import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/token")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"})
public class TokenController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Token>> generateToken(
            @RequestParam Long userId,
            @RequestParam String sectorType,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long counterId,
            @RequestParam(required = false) Long referenceId,
            @RequestParam String serviceName,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false, defaultValue = "REGULAR") String priority) {
        if ("HOSPITAL".equalsIgnoreCase(sectorType) && referenceId != null) {
            Doctor doctor = doctorRepository.findById(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found for legacy hospital token request."));
            branchId = doctor.getBranch().getId();
        } else if (branchId == null) {
            branchId = referenceId;
        }

        if (counterId == null) {
            counterId = inferCounterId(sectorType, branchId, referenceId, serviceName, priority);
        }
        Token token = tokenService.generateToken(userId, sectorType, branchId, counterId, serviceName, appointmentId, priority);
        populateTokenDetails(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token generated successfully!", token));
    }

    private Long inferCounterId(String sectorType, Long branchId, Long referenceId, String serviceName, String priority) {
        sectorType = sectorType.toUpperCase();
        String normalizedService = serviceName == null ? "" : serviceName.toLowerCase();
        if (branchId == null && referenceId != null) {
            branchId = referenceId;
        }

        if (sectorType.equals("HOSPITAL")) {
            if (referenceId != null) {
                Doctor doctor = doctorRepository.findById(referenceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Doctor not found for legacy hospital token request."));
                branchId = doctor.getBranch().getId();
            }
            if ("EMERGENCY".equalsIgnoreCase(priority) || normalizedService.contains("emergency")) {
                return findCounterByType("HOSPITAL", branchId, "Emergency Counter");
            }
            if (normalizedService.contains("bill") || normalizedService.contains("payment") || normalizedService.contains("billing")) {
                return findCounterByType("HOSPITAL", branchId, "Billing Counter");
            }
            return findCounterByType("HOSPITAL", branchId, "OPD Counter");
        }

        if (sectorType.equals("BANK")) {
            if (normalizedService.contains("loan")) {
                return findCounterByType("BANK", branchId, "Loan Desk");
            }
            if (normalizedService.contains("kyc") || normalizedService.contains("card")) {
                return findCounterByType("BANK", branchId, "KYC Desk");
            }
            return findCounterByType("BANK", branchId, "Cash Counter");
        }

        if (sectorType.equals("COLLEGE")) {
            if (normalizedService.contains("scholarship")) {
                return findCounterByType("COLLEGE", branchId, "Scholarship Desk");
            }
            if (normalizedService.contains("admission")) {
                return findCounterByType("COLLEGE", branchId, "Admission Desk");
            }
            return findCounterByType("COLLEGE", branchId, "Fees Counter");
        }

        throw new ResourceNotFoundException("Unable to infer counter for sector type: " + sectorType);
    }

    private Long findCounterByType(String sectorType, Long branchId, String counterType) {
        List<Counter> counters = counterRepository.findByTypeAndReferenceId(sectorType, branchId);
        if (counters.isEmpty()) {
            throw new ResourceNotFoundException("No active counters found for branch " + branchId);
        }
        return counters.stream()
                .filter(counter -> counterType.equalsIgnoreCase(counter.getCounterType()))
                .findFirst()
                .map(Counter::getId)
                .orElse(counters.get(0).getId());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Token>>> getUserTokens(@PathVariable Long userId) {
        List<Token> tokens = tokenService.getTokensByUserId(userId);
        if (tokens != null) {
            tokens.forEach(this::populateTokenDetails);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Tokens fetched successfully!", tokens));
    }

    @PostMapping("/{tokenId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelToken(@PathVariable Long tokenId) {
        tokenService.cancelToken(tokenId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token cancelled successfully!", null));
    }

    @GetMapping("/{tokenId}/position")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTokenPosition(@PathVariable Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));

        Map<String, Object> data = new HashMap<>();
        data.put("tokenNumber", token.getTokenNumber());
        data.put("status", token.getStatus());

        if (!token.getStatus().equals("WAITING")) {
            data.put("peopleAhead", 0);
            data.put("waitingTime", 0);
            data.put("servingToken", token.getStatus().equals("SERVING") ? token.getTokenNumber() : "None");
            return ResponseEntity.ok(new ApiResponse<>(true, "Position fetched successfully!", data));
        }

        // Count people ahead
        List<Token> waitingTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterIdAndStatus(
                token.getSectorType(), token.getBranchId(), token.getCounterId(), "WAITING");

        // Sort just like callNextToken: EMERGENCY first, then ID
        List<Token> sortedWaiting = waitingTokens.stream()
                .sorted((t1, t2) -> {
                    if (t1.getPriority().equals("EMERGENCY") && !t2.getPriority().equals("EMERGENCY")) {
                        return -1;
                    } else if (!t1.getPriority().equals("EMERGENCY") && t2.getPriority().equals("EMERGENCY")) {
                        return 1;
                    }
                    return t1.getId().compareTo(t2.getId());
                })
                .collect(Collectors.toList());

        int position = sortedWaiting.indexOf(token);
        int peopleAhead = Math.max(0, position);

        int serviceTime = token.getSectorType().equals("HOSPITAL") ? 15 : (token.getSectorType().equals("BANK") ? 8 : 10);
        int waitingTime = peopleAhead * serviceTime;

        Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(token.getSectorType(), token.getBranchId(), token.getCounterId()).orElse(null);
        String servingToken = (queue != null && queue.getCurrentServingToken() != null) ? queue.getCurrentServingToken().getTokenNumber() : "None";

        data.put("peopleAhead", peopleAhead);
        data.put("waitingTime", waitingTime);
        data.put("servingToken", servingToken);

        return ResponseEntity.ok(new ApiResponse<>(true, "Position fetched successfully!", data));
    }

    private void populateTokenDetails(Token token) {
        if (token == null) return;
        
        if ("HOSPITAL".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                hospitalBranchRepository.findById(token.getBranchId()).ifPresent(branch -> {
                    String hospName = branch.getHospital() != null ? branch.getHospital().getName() : "";
                    token.setHospitalName(hospName + " (" + branch.getName() + ")");
                });
            }
            if (token.getAppointment() != null && token.getAppointment().getReferenceId() != null) {
                doctorRepository.findById(token.getAppointment().getReferenceId()).ifPresent(doctor -> {
                    token.setDoctorName(doctor.getName() + " (" + doctor.getSpecialization() + ")");
                });
            }
        } else if ("BANK".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                bankBranchRepository.findById(token.getBranchId()).ifPresent(branch -> {
                    String bankName = branch.getBank() != null ? branch.getBank().getName() : "";
                    token.setHospitalName(bankName + " (" + branch.getName() + ")");
                });
            }
        } else if ("COLLEGE".equalsIgnoreCase(token.getSectorType())) {
            if (token.getBranchId() != null) {
                departmentRepository.findById(token.getBranchId()).ifPresent(dept -> {
                    String collegeName = dept.getCollege() != null ? dept.getCollege().getName() : "";
                    token.setHospitalName(collegeName + " (" + dept.getName() + ")");
                });
            }
        }
    }
}
