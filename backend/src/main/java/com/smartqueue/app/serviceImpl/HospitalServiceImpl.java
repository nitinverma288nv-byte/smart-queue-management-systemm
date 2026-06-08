package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.HospitalService;
import com.smartqueue.app.service.HospitalService;
import com.smartqueue.app.service.NotificationService;
import com.smartqueue.app.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalBranchRepository hospitalBranchRepository;

    @Autowired
    private DoctorRepository doctorRepository;

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
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    @Override
    public List<HospitalBranch> getBranchesByHospital(Long hospitalId) {
        return hospitalBranchRepository.findByHospitalId(hospitalId);
    }

    @Override
    public List<Doctor> getDoctorsByBranch(Long branchId) {
        return doctorRepository.findByBranchId(branchId);
    }

    @Override
    public List<Doctor> getDoctorsByHospital(Long hospitalId) {
        return doctorRepository.findByHospitalId(hospitalId);
    }

    @Override
    public List<Slot> getSlotsByDoctorAndDate(Long doctorId, LocalDate date) {
        return slotRepository.findByTypeAndReferenceIdAndDate("DOCTOR", doctorId, date);
    }

    @Autowired
    private BookingTransactionHelper bookingTransactionHelper;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Appointment bookAppointment(Long userId, Long slotId, Long doctorId, String serviceName) {
        int maxRetries = 5;
        long delay = 100;
        org.springframework.dao.ConcurrencyFailureException lastEx = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Appointment appt = bookingTransactionHelper.executeHospitalBooking(userId, slotId, doctorId, serviceName);
                
                User user = appt.getUser();
                Slot slot = appt.getSlot();
                notificationService.sendNotification(userId, "Your Hospital appointment is booked successfully. Slot: " + slot.getStartTime() + " - " + slot.getEndTime());
                
                if (emailService != null && user.getEmail() != null) {
                    new Thread(() -> {
                        try {
                            emailService.sendAppointmentTimingEmail(user.getEmail(), user.getFullName(), serviceName, slot.getStartTime() + " - " + slot.getEndTime(), "HOSPITAL");
                        } catch (Exception ex) {
                            System.err.println("❌ Background hospital booking email dispatch error: " + ex.getMessage());
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

        notificationService.sendNotification(appointment.getUser().getId(), "Your Hospital appointment has been cancelled successfully.");
    }
}
