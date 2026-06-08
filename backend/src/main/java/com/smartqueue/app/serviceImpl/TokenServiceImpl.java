package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.NotificationService;
import com.smartqueue.app.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.smartqueue.app.controller.SseController;


@Service
@Transactional
public class TokenServiceImpl implements TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TokenSequenceRepository tokenSequenceRepository;

    @Autowired
    private com.smartqueue.app.service.EmailService emailService;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private String getTokenPrefix(String serviceName, String sectorType) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return sectorType.substring(0, 1).toUpperCase();
        }
        String s = serviceName.toLowerCase();
        if (s.contains("cold")) return "COLD";
        if (s.contains("fever")) return "FEVER";
        if (s.contains("headache")) return "HEAD";
        if (s.contains("allergy") || s.contains("skin")) return "ALLERGY";
        if (s.contains("dental")) return "DENTAL";
        if (s.contains("ortho")) return "ORTHO";
        if (s.contains("cardio")) return "CARDIO";
        if (s.contains("neuro")) return "NEURO";
        if (s.contains("emergency") || s.contains("emg")) return "EMG";
        if (s.contains("ent")) return "ENT";
        if (s.contains("diabet")) return "DIABET";
        if (s.contains("eye")) return "EYE";
        if (s.contains("checkup") || s.contains("consult")) return "CONSULT";
        
        // Bank services
        if (s.contains("cash") || s.contains("withdraw") || s.contains("deposit")) return "CASH";
        if (s.contains("kyc")) return "KYC";
        if (s.contains("loan")) return "LOAN";
        if (s.contains("card")) return "CARD";
        
        // College services
        if (s.contains("scholar")) return "SCHOL";
        if (s.contains("admission")) return "ADM";
        if (s.contains("fees") || s.contains("payment")) return "FEES";
        
        // Default
        return sectorType.substring(0, 3).toUpperCase();
    }

    private int getPriorityWeight(String priority) {
        if (priority == null) return 1;
        String p = priority.toUpperCase();
        if (p.equals("EMERGENCY")) return 3;
        if (p.equals("SENIOR") || p.equals("SENIOR_CITIZEN")) return 2;
        return 1; // REGULAR
    }

    @Override
    @Transactional
    public Token generateToken(Long userId, String sectorType, Long branchId, Long counterId, String serviceName, Long appointmentId, String priority) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found!"));
            appointment.setStatus("CONFIRMED");
            appointmentRepository.save(appointment);
        }

        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found!"));
        if (!counter.getSectorType().equalsIgnoreCase(sectorType)) {
            throw new BadRequestException("Counter sector does not match requested sector type.");
        }
        if (!counter.getBranchId().equals(branchId)) {
            throw new BadRequestException("Counter branch does not match requested branch.");
        }

        Long assignedStaffId = counter.getAssignedStaffId();
        String prefix = getTokenPrefix(serviceName, sectorType);
        
        final int initialVal;
        if ("CARDIO".equals(prefix)) {
            initialVal = 200;
        } else if ("EMG".equals(prefix)) {
            initialVal = 0;
        } else {
            initialVal = 100;
        }

        // 1. Lock the sequence record for the prefix in database to serialize concurrent bookings
        TokenSequence sequence = tokenSequenceRepository.findBySectorTypeForUpdate(prefix)
                .orElseGet(() -> {
                    TokenSequence seq = TokenSequence.builder()
                            .sectorType(prefix)
                            .lastValue(initialVal)
                            .build();
                    return tokenSequenceRepository.save(seq);
                });

        // 2. Perform "latest token lookup"
        int nextNum = sequence.getLastValue() + 1;

        // 3. Update the sequence record
        sequence.setLastValue(nextNum);
        tokenSequenceRepository.save(sequence);

        // 4. Get or Create Queue and update its lastTokenNumber field
        Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(sectorType.toUpperCase(), branchId, counterId)
                .orElseGet(() -> {
                    Queue newQueue = Queue.builder()
                            .sectorType(sectorType.toUpperCase())
                            .referenceId(branchId)
                            .branchId(branchId)
                            .counterId(counterId)
                            .assignedStaffId(assignedStaffId)
                            .lastTokenNumber(100)
                            .build();
                    return queueRepository.save(newQueue);
                });
        queue.setLastTokenNumber(nextNum);
        queue.setAssignedStaffId(assignedStaffId);
        queueRepository.save(queue);

        // 5. Generate and save the unique token with custom formats
        String tokenNumber;
        if ("EMG".equals(prefix)) {
            tokenNumber = prefix + "-" + String.format("%03d", nextNum);
        } else {
            tokenNumber = prefix + "-" + nextNum;
        }

        Token token = Token.builder()
                .tokenNumber(tokenNumber)
                .user(user)
                .appointment(appointment)
                .sectorType(sectorType.toUpperCase())
                .referenceId(branchId)
                .branchId(branchId)
                .counterId(counterId)
                .assignedStaffId(assignedStaffId)
                .serviceName(serviceName)
                .status("WAITING")
                .priority(priority != null ? priority.toUpperCase() : "REGULAR")
                .build();

        Token savedToken = tokenRepository.save(token);

        // Notify user via in-app notification
        notificationService.sendNotification(userId, "Token generated successfully: " + tokenNumber + ". Your priority is " + token.getPriority() + ".");

        // Dispatch immediate Gmail booking reminder with full token details
        if (emailService != null && user.getEmail() != null) {
            try {
                String location = "Main Desk";
                if ("HOSPITAL".equalsIgnoreCase(sectorType)) {
                    if (branchId != null) {
                        hospitalBranchRepository.findById(branchId).ifPresent(branch -> {
                            String hospName = branch.getHospital() != null ? branch.getHospital().getName() : "";
                            token.setHospitalName(hospName + " (" + branch.getName() + ")");
                        });
                    }
                    if (appointment != null && appointment.getReferenceId() != null) {
                        doctorRepository.findById(appointment.getReferenceId()).ifPresent(doctor -> {
                            token.setDoctorName(doctor.getName() + " (" + doctor.getSpecialization() + ")");
                        });
                    }
                    location = token.getHospitalName() != null ? token.getHospitalName() : "Main Hospital Branch";
                } else if ("BANK".equalsIgnoreCase(sectorType)) {
                    if (branchId != null) {
                        bankBranchRepository.findById(branchId).ifPresent(branch -> {
                            String bankName = branch.getBank() != null ? branch.getBank().getName() : "";
                            token.setHospitalName(bankName + " (" + branch.getName() + ")");
                        });
                    }
                    location = token.getHospitalName() != null ? token.getHospitalName() : "Main Bank Branch";
                } else if ("COLLEGE".equalsIgnoreCase(sectorType)) {
                    if (branchId != null) {
                        departmentRepository.findById(branchId).ifPresent(dept -> {
                            String collegeName = dept.getCollege() != null ? dept.getCollege().getName() : "";
                            token.setHospitalName(collegeName + " (" + dept.getName() + ")");
                        });
                    }
                    location = token.getHospitalName() != null ? token.getHospitalName() : "Main College Desk";
                }

                String timing = "Direct Entry Queue";
                if (appointment != null && appointment.getSlot() != null) {
                    Slot slot = appointment.getSlot();
                    timing = slot.getStartTime() + " - " + slot.getEndTime() + " on " + slot.getDate();
                }

                final String finalLocation = location;
                final String finalTiming = timing;
                new Thread(() -> {
                    try {
                        emailService.sendTokenBookingEmail(
                            user.getEmail(),
                            user.getFullName(),
                            tokenNumber,
                            sectorType.toUpperCase(),
                            serviceName,
                            finalTiming,
                            token.getPriority(),
                            finalLocation
                        );
                    } catch (Exception ex) {
                        System.err.println("❌ Background email dispatch error: " + ex.getMessage());
                    }
                }).start();
            } catch (Exception ex) {
                System.err.println("❌ Failed to initiate token booking email: " + ex.getMessage());
            }
        }

        SseController.notifyQueueUpdate();
        return savedToken;
    }

    @Override
    public List<Token> getTokensByUserId(Long userId) {
        return tokenRepository.findByUserId(userId);
    }

    @Override
    public Token updateTokenStatus(Long tokenId, String status) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));
        token.setStatus(status.toUpperCase());
        Token savedToken = tokenRepository.save(token);

        notificationService.sendNotification(token.getUser().getId(), "Your token " + token.getTokenNumber() + " status is updated to " + status + ".");
        SseController.notifyQueueUpdate();
        return savedToken;
    }

    @Override
    public Token callNextToken(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found!"));

        List<Token> waitingTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterIdAndStatus(
                counter.getSectorType(), counter.getBranchId(), counter.getId(), "WAITING");

        if (waitingTokens.isEmpty()) {
            Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(counter.getSectorType(), counter.getBranchId(), counter.getId()).orElse(null);
            if (queue != null) {
                queue.setCurrentServingToken(null);
                queueRepository.save(queue);
            }
            return null;
        }

        // Sort: EMERGENCY (3) first, then SENIOR (2), then REGULAR (1), then by ID (FIFO)
        Token nextToken = waitingTokens.stream()
                .sorted((t1, t2) -> {
                    int w1 = getPriorityWeight(t1.getPriority());
                    int w2 = getPriorityWeight(t2.getPriority());
                    if (w1 != w2) {
                        return Integer.compare(w2, w1);
                    }
                    return t1.getId().compareTo(t2.getId());
                })
                .findFirst()
                .orElse(null);

        if (nextToken != null) {
            nextToken.setStatus("SERVING");
            nextToken.setAssignedStaffId(counter.getAssignedStaffId());
            tokenRepository.save(nextToken);

            Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(counter.getSectorType(), counter.getBranchId(), counter.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Queue not found!"));
            queue.setCurrentServingToken(nextToken);
            queue.setUserId(nextToken.getUser().getId());
            queueRepository.save(queue);

            notificationService.sendNotification(nextToken.getUser().getId(), "Your token " + nextToken.getTokenNumber() + " is now being served! Please proceed to the counter.");
            notifyWaitingUsers(counter.getSectorType(), counter.getBranchId(), counter.getId());
        }

        SseController.notifyQueueUpdate();
        return nextToken;
    }

    private void notifyWaitingUsers(String sectorType, Long branchId, Long counterId) {
        List<Token> waitingTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterIdAndStatus(sectorType, branchId, counterId, "WAITING");
        for (Token t : waitingTokens) {
            notificationService.sendNotification(t.getUser().getId(), "Queue update: The queue has advanced. Check your new position.");
        }
    }

    @Override
    public Token skipToken(Long tokenId) {
        Token token = updateTokenStatus(tokenId, "SKIPPED");
        // Auto-call next
        if (token.getCounterId() == null) {
            throw new BadRequestException("Token is not assigned to a counter.");
        }
        callNextToken(token.getCounterId());
        SseController.notifyQueueUpdate();
        return token;
    }

    @Override
    public Token completeToken(Long tokenId) {
        Token token = updateTokenStatus(tokenId, "COMPLETED");
        // Auto-call next
        if (token.getCounterId() == null) {
            throw new BadRequestException("Token is not assigned to a counter.");
        }
        callNextToken(token.getCounterId());
        SseController.notifyQueueUpdate();
        return token;
    }

    @Override
    public void cancelToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));
        token.setStatus("CANCELLED");
        tokenRepository.save(token);

        notificationService.sendNotification(token.getUser().getId(), "Your token " + token.getTokenNumber() + " has been cancelled.");
        if (token.getCounterId() != null) {
            notifyWaitingUsers(token.getSectorType(), token.getBranchId(), token.getCounterId());
        }
        SseController.notifyQueueUpdate();
    }

    @Override
    public List<Token> getActiveQueueTokens(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found!"));
        return tokenRepository.findBySectorTypeAndBranchIdAndCounterId(counter.getSectorType(), counter.getBranchId(), counter.getId());
    }

    @Override
    public Token callSpecificToken(Long tokenId, Long counterId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found!"));
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found!"));
        
        token.setStatus("SERVING");
        token.setCounterId(counterId);
        token.setBranchId(counter.getBranchId());
        token.setAssignedStaffId(counter.getAssignedStaffId());
        tokenRepository.save(token);

        Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(counter.getSectorType(), counter.getBranchId(), counter.getId())
                .orElseGet(() -> {
                    Queue newQueue = Queue.builder()
                            .sectorType(counter.getSectorType().toUpperCase())
                            .referenceId(counter.getBranchId())
                            .branchId(counter.getBranchId())
                            .counterId(counterId)
                            .assignedStaffId(counter.getAssignedStaffId())
                            .lastTokenNumber(100)
                            .build();
                    return queueRepository.save(newQueue);
                });
        queue.setCurrentServingToken(token);
        queue.setUserId(token.getUser().getId());
        queueRepository.save(queue);

        notificationService.sendNotification(token.getUser().getId(), "Your token " + token.getTokenNumber() + " is now being served! Please proceed to the counter.");
        notifyWaitingUsers(counter.getSectorType(), counter.getBranchId(), counter.getId());
        SseController.notifyQueueUpdate();

        return token;
    }
}
