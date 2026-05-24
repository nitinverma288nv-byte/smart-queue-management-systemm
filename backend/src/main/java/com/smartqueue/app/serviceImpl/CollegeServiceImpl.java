package com.smartqueue.app.serviceImpl;

import com.smartqueue.app.entity.*;
import com.smartqueue.app.exception.BadRequestException;
import com.smartqueue.app.exception.ResourceNotFoundException;
import com.smartqueue.app.repository.*;
import com.smartqueue.app.service.CollegeService;
import com.smartqueue.app.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CollegeServiceImpl implements CollegeService {

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public List<College> getAllColleges() {
        return collegeRepository.findAll();
    }

    @Override
    public List<Department> getDepartmentsByCollege(Long collegeId) {
        return departmentRepository.findByCollegeId(collegeId);
    }

    @Override
    public List<Slot> getSlotsByDepartmentAndDate(Long departmentId, LocalDate date) {
        return slotRepository.findByTypeAndBranchIdAndDate("COLLEGE", departmentId, date);
    }

    @Override
    public Appointment bookAppointment(Long userId, Long slotId, Long departmentId, String serviceName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found!"));

        if (slot.getBookedTokens() >= slot.getMaxTokens()) {
            throw new BadRequestException("This slot is fully booked!");
        }

        slot.setBookedTokens(slot.getBookedTokens() + 1);
        slotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .user(user)
                .slot(slot)
                .sectorType("COLLEGE")
                .referenceId(departmentId)
                .serviceName(serviceName)
                .status("PENDING")
                .build();

        Appointment savedAppt = appointmentRepository.save(appointment);
        notificationService.sendNotification(userId, "Your College request for " + serviceName + " is booked successfully. Slot: " + slot.getStartTime() + " - " + slot.getEndTime());

        return savedAppt;
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

        notificationService.sendNotification(appointment.getUser().getId(), "Your College request for " + appointment.getServiceName() + " has been cancelled.");
    }
}
