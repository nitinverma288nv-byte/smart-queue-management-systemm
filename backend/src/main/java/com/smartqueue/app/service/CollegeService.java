package com.smartqueue.app.service;

import com.smartqueue.app.entity.College;
import com.smartqueue.app.entity.Department;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface CollegeService {
    List<College> getAllColleges();
    List<Department> getDepartmentsByCollege(Long collegeId);
    List<Slot> getSlotsByDepartmentAndDate(Long departmentId, LocalDate date);
    Appointment bookAppointment(Long userId, Long slotId, Long departmentId, String serviceName);
    void cancelAppointment(Long appointmentId);
}
