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

    @Override
    @Transactional
    public synchronized Token generateToken(Long userId, String sectorType, Long branchId, Long counterId, String serviceName, Long appointmentId, String priority) {
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

        // 1. Lock the sequence record for the sector in database to serialize concurrent bookings
        TokenSequence sequence = tokenSequenceRepository.findBySectorTypeForUpdate(sectorType.toUpperCase())
                .orElseGet(() -> {
                    TokenSequence seq = TokenSequence.builder()
                            .sectorType(sectorType.toUpperCase())
                            .lastValue(100)
                            .build();
                    return tokenSequenceRepository.save(seq);
                });

        // 2. Perform "latest token lookup" as an extra layer of defense
        int nextNum = sequence.getLastValue() + 1;
        java.util.Optional<Token> latestTokenOpt = tokenRepository.findFirstBySectorTypeOrderByIdDesc(sectorType.toUpperCase());
        if (latestTokenOpt.isPresent()) {
            Token latestToken = latestTokenOpt.get();
            String latestTokenNumber = latestToken.getTokenNumber();
            int dashIndex = latestTokenNumber.indexOf("-");
            if (dashIndex != -1) {
                try {
                    int num = Integer.parseInt(latestTokenNumber.substring(dashIndex + 1));
                    if (num >= nextNum) {
                        nextNum = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

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

        // 5. Generate and save the unique token
        String prefix = sectorType.substring(0, 1).toUpperCase(); // H, B, or C
        String tokenNumber = prefix + "-" + nextNum;

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

        // Notify user
        notificationService.sendNotification(userId, "Token generated successfully: " + tokenNumber + ". Your priority is " + token.getPriority() + ".");

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

        // Sort: EMERGENCY first, then by ID (FIFO)
        Token nextToken = waitingTokens.stream()
                .sorted((t1, t2) -> {
                    if (t1.getPriority().equals("EMERGENCY") && !t2.getPriority().equals("EMERGENCY")) {
                        return -1;
                    } else if (!t1.getPriority().equals("EMERGENCY") && t2.getPriority().equals("EMERGENCY")) {
                        return 1;
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

        return token;
    }
}
