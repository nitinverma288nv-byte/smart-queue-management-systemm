package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.BankService;
import com.smartqueue.app.service.BankService;
import com.smartqueue.app.service.NotificationService;
import com.smartqueue.app.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class BankServiceImpl implements BankService {

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private BankBranchRepository bankBranchRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Override
    public List<Bank> getAllBanks() {
        return bankRepository.findAll();
    }

    @Override
    public List<BankBranch> getBranchesByBank(Long bankId) {
        return bankBranchRepository.findByBankId(bankId);
    }

    @Override
    public List<Counter> getCountersByBranch(Long branchId) {
        return counterRepository.findByTypeAndReferenceId("BANK", branchId);
    }

    @Override
    public List<Slot> getSlotsByCounterAndDate(Long counterId, LocalDate date) {
        List<Slot> slots = slotRepository.findByTypeAndReferenceIdAndDate("BANK", counterId, date);
        if (slots.isEmpty()) {
            Counter counter = counterRepository.findById(counterId).orElse(null);
            if (counter != null) {
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slotRepository.save(Slot.builder().type("BANK").referenceId(counterId).sectorType("BANK").branchId(counter.getBranchId()).counterId(counterId).date(date).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).maxTokens(10).bookedTokens(0).availability(true).maxCapacity(10).build());
                slots = slotRepository.findByTypeAndReferenceIdAndDate("BANK", counterId, date);
            }
        }
        return slots;
    }

    @Autowired
    private BookingTransactionHelper bookingTransactionHelper;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Appointment bookAppointment(Long userId, Long slotId, Long branchId, String serviceName) {
        int maxRetries = 5;
        long delay = 100;
        org.springframework.dao.ConcurrencyFailureException lastEx = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Appointment appt = bookingTransactionHelper.executeBankBooking(userId, slotId, branchId, serviceName);
                
                User user = appt.getUser();
                Slot slot = appt.getSlot();
                notificationService.sendNotification(userId, "Your Bank appointment is booked successfully for " + serviceName + ". Slot: " + slot.getStartTime() + " - " + slot.getEndTime());
                
                if (emailService != null && user.getEmail() != null) {
                    new Thread(() -> {
                        try {
                            emailService.sendAppointmentTimingEmail(user.getEmail(), user.getFullName(), serviceName, slot.getStartTime() + " - " + slot.getEndTime(), "BANK");
                        } catch (Exception ex) {
                            System.err.println("❌ Background bank booking email dispatch error: " + ex.getMessage());
                        }
                    }).start();
                }
                
                com.smartqueue.app.controller.SseController.notifyQueueUpdate();
                return appt;
            } catch (org.springframework.dao.ConcurrencyFailureException ex) {
                lastEx = ex;
                try {
                    Thread.sleep(delay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw new BadRequestException("The booking system is currently experiencing high load. Please try booking again in a moment.");
    }

    @Override
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found!"));
        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);

        Slot slot = appointment.getSlot();
        if (slot.getBookedTokens() > 0) {
            slot.setBookedTokens(slot.getBookedTokens() - 1);
            slotRepository.save(slot);
        }

        notificationService.sendNotification(appointment.getUser().getId(), "Your Bank appointment for " + appointment.getServiceName() + " has been cancelled.");
    }
}
