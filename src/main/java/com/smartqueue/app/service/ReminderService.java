package com.smartqueue.app.service;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.controller.SseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private int getPriorityWeight(String priority) {
        if (priority == null) return 1;
        String p = priority.toUpperCase();
        if (p.equals("EMERGENCY")) return 3;
        if (p.equals("SENIOR") || p.equals("SENIOR_CITIZEN")) return 2;
        return 1;
    }

    @Scheduled(fixedRate = 60000) // Runs every minute
    public void sendUpcomingReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 1. Fetch all tokens that are WAITING across all sectors
        List<Token> waitingTokensGlobal = tokenRepository.findBySectorTypeAndStatus("HOSPITAL", "WAITING");
        waitingTokensGlobal.addAll(tokenRepository.findBySectorTypeAndStatus("BANK", "WAITING"));
        waitingTokensGlobal.addAll(tokenRepository.findBySectorTypeAndStatus("COLLEGE", "WAITING"));

        for (Token token : waitingTokensGlobal) {
            if (!"WAITING".equalsIgnoreCase(token.getStatus())) {
                continue;
            }

            // 2. Count people ahead at their specific counter queue
            List<Token> counterWaitingTokens = tokenRepository.findBySectorTypeAndBranchIdAndCounterIdAndStatus(
                    token.getSectorType(), token.getBranchId(), token.getCounterId(), "WAITING");

            List<Token> sortedWaiting = counterWaitingTokens.stream()
                    .sorted((t1, t2) -> {
                        int w1 = getPriorityWeight(t1.getPriority());
                        int w2 = getPriorityWeight(t2.getPriority());
                        if (w1 != w2) {
                            return Integer.compare(w2, w1);
                        }
                        return t1.getId().compareTo(t2.getId());
                    })
                    .collect(Collectors.toList());

            int position = sortedWaiting.indexOf(token);
            int peopleAhead = Math.max(0, position);

            int serviceTime = token.getSectorType().equals("HOSPITAL") ? 15 : (token.getSectorType().equals("BANK") ? 8 : 10);
            int dynamicQueueWaitMins = peopleAhead * serviceTime;

            long expectedMins = dynamicQueueWaitMins;
            String slotTime = "Direct Queue Entry";

            // If it is a slot appointment, adjust expected minutes based on slot start time
            if (token.getAppointment() != null && token.getAppointment().getSlot() != null) {
                Slot slot = token.getAppointment().getSlot();
                if (!slot.getDate().equals(today)) {
                    // Skip reminders if appointment is scheduled for another day
                    continue;
                }
                
                LocalTime slotStartTime = slot.getStartTime();
                long minutesUntilSlot = ChronoUnit.MINUTES.between(now, slotStartTime);
                
                // Expected remaining time is the maximum of the slot scheduled time and actual queue delays
                expectedMins = Math.max(minutesUntilSlot, (long) dynamicQueueWaitMins);
                slotTime = slotStartTime + " - " + slot.getEndTime();

                // If appointment is already expired, skip reminder
                if (minutesUntilSlot < -60) { // expired more than 1 hour ago
                    continue;
                }
            }

            // If wait time is negative (e.g. appointment time passed but still waiting), set to 0
            if (expectedMins < 0) {
                expectedMins = 0;
            }

            // Define trigger thresholds: 120m (2 hours), 60m (1 hour), 30m, 15m, 5m
            if (expectedMins <= 120 && expectedMins > 60) {
                triggerReminder(token, 120, expectedMins, slotTime);
            } else if (expectedMins <= 60 && expectedMins > 30) {
                triggerReminder(token, 60, expectedMins, slotTime);
            } else if (expectedMins <= 30 && expectedMins > 15) {
                triggerReminder(token, 30, expectedMins, slotTime);
            } else if (expectedMins <= 15 && expectedMins > 5) {
                triggerReminder(token, 15, expectedMins, slotTime);
            } else if (expectedMins <= 5 && expectedMins >= 0) {
                triggerReminder(token, 5, expectedMins, slotTime);
            }
        }
    }

    private void triggerReminder(Token token, int thresholdMinutes, long actualWaitMins, String slotTime) {
        User user = token.getUser();
        Long userId = user.getId();
        String tokenNum = token.getTokenNumber();

        // 1. Persistently prevent duplicate notifications using Notification table
        String matchKey = "[Token: " + tokenNum + " - " + thresholdMinutes + "m]";
        boolean alreadySent = notificationRepository.existsByUserIdAndMessageContaining(userId, matchKey);
        if (alreadySent) {
            return;
        }

        // Get details (Branch and Counter)
        String branchName = "Main Branch";
        if ("HOSPITAL".equalsIgnoreCase(token.getSectorType())) {
            HospitalBranch branch = hospitalBranchRepository.findById(token.getBranchId()).orElse(null);
            if (branch != null) {
                String hospName = branch.getHospital() != null ? branch.getHospital().getName() : "Hospital";
                branchName = hospName + " (" + branch.getName() + ")";
            }
        } else if ("BANK".equalsIgnoreCase(token.getSectorType())) {
            BankBranch branch = bankBranchRepository.findById(token.getBranchId()).orElse(null);
            if (branch != null) {
                String bankName = branch.getBank() != null ? branch.getBank().getName() : "Bank";
                branchName = bankName + " (" + branch.getName() + ")";
            }
        } else if ("COLLEGE".equalsIgnoreCase(token.getSectorType())) {
            Department dept = departmentRepository.findById(token.getBranchId()).orElse(null);
            if (dept != null) {
                String collegeName = dept.getCollege() != null ? dept.getCollege().getName() : "College";
                branchName = collegeName + " (" + dept.getName() + ")";
            }
        }

        Counter counter = counterRepository.findById(token.getCounterId()).orElse(null);
        String counterName = counter != null ? (counter.getCounterName() != null ? counter.getCounterName() : "Counter " + counter.getId()) : "Main Desk";

        // Current Queue status
        Queue queue = queueRepository.findBySectorTypeAndBranchIdAndCounterId(token.getSectorType(), token.getBranchId(), token.getCounterId()).orElse(null);
        String servingToken = (queue != null && queue.getCurrentServingToken() != null) ? queue.getCurrentServingToken().getTokenNumber() : "None";

        // Compose highly premium, content-rich notification text
        String message = String.format("🔔 Real-Time Reminder %s: Your appointment at %s (Counter: %s) for %s begins in %d minutes. Slot Time: %s. Queue Status: Currently serving %s. Est. Remaining wait time: %d mins.",
                matchKey, branchName, counterName, token.getServiceName(), actualWaitMins, slotTime, servingToken, actualWaitMins);

        // 2. Log Notification to database for audit history
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // 3. Dispatch SMS (Simulated Log)
        System.out.println("📱 [SMS Simulated Dispatch] To: " + user.getUsername() + " | Phone: " + user.getEmail() + " | Message: " + message);

        // 4. Dispatch Email (Standard SMTP via Gmail)
        if (emailService != null && user.getEmail() != null) {
            try {
                emailService.sendAppointmentTimingEmail(
                        user.getEmail(),
                        user.getFullName(),
                        token.getServiceName() + " (" + thresholdMinutes + "m Reminder)",
                        slotTime + " [Est. Wait: " + actualWaitMins + " mins]",
                        token.getSectorType()
                );
            } catch (Exception e) {
                System.err.println("❌ SMTP Reminder Email failed: " + e.getMessage());
            }
        }

        // 5. Dispatch Live Browser SSE Push
        try {
            SseController.sendToUser(userId.toString(), "NOTIFICATION", "{\"message\":\"" + message + "\"}");
        } catch (Exception e) {
            System.err.println("❌ SSE notification push failed: " + e.getMessage());
        }
    }
}
