package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingTransactionHelper {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Appointment executeBankBooking(Long userId, Long slotId, Long branchId, String serviceName) {
        // Enforce Row-Level Pessimistic Locking on slot retrieval immediately
        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found!"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        // Atomic capacity validation
        if (slot.getBookedTokens() >= slot.getMaxTokens()) {
            throw new BadRequestException("This slot is already fully booked!");
        }

        // Duplicate active booking check
        boolean exists = appointmentRepository.existsByUserIdAndSlotIdAndStatusNot(userId, slotId, "CANCELLED");
        if (exists) {
            throw new BadRequestException("You already have an active appointment booked for this slot!");
        }

        // Update slot capacity
        slot.setBookedTokens(slot.getBookedTokens() + 1);
        slotRepository.save(slot);

        // Build and save appointment
        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .sectorType("BANK")
                .referenceId(branchId)
                .serviceName(serviceName)
                .status("PENDING")
                .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Appointment executeHospitalBooking(Long userId, Long slotId, Long doctorId, String serviceName) {
        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found!"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (slot.getBookedTokens() >= slot.getMaxTokens()) {
            throw new BadRequestException("This slot is already fully booked!");
        }

        // Specialized duplicate active booking check for Specialist + Date
        boolean hasDuplicate = appointmentRepository.findByUserId(userId).stream()
                .anyMatch(appt -> !"CANCELLED".equalsIgnoreCase(appt.getStatus())
                        && appt.getReferenceId().equals(doctorId)
                        && appt.getServiceName().equalsIgnoreCase(serviceName)
                        && appt.getSlot().getDate().equals(slot.getDate()));
        
        if (hasDuplicate) {
            throw new BadRequestException("You already have an active appointment for this service with this specialist today!");
        }

        slot.setBookedTokens(slot.getBookedTokens() + 1);
        slotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .sectorType("HOSPITAL")
                .referenceId(doctorId)
                .serviceName(serviceName)
                .status("PENDING")
                .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Appointment executeCollegeBooking(Long userId, Long slotId, Long branchId, String serviceName) {
        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found!"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (slot.getBookedTokens() >= slot.getMaxTokens()) {
            throw new BadRequestException("This slot is already fully booked!");
        }

        boolean exists = appointmentRepository.existsByUserIdAndSlotIdAndStatusNot(userId, slotId, "CANCELLED");
        if (exists) {
            throw new BadRequestException("You already have an active appointment booked for this slot!");
        }

        slot.setBookedTokens(slot.getBookedTokens() + 1);
        slotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .sectorType("COLLEGE")
                .referenceId(branchId)
                .serviceName(serviceName)
                .status("PENDING")
                .build();

        return appointmentRepository.save(appointment);
    }
}
