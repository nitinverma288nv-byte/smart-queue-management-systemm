package com.smartqueue.app.service;

import com.smartqueue.app.entity.Hospital;
import com.smartqueue.app.entity.HospitalBranch;
import com.smartqueue.app.entity.Doctor;
import com.smartqueue.app.entity.Slot;
import com.smartqueue.app.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface HospitalService {
    List<Hospital> getAllHospitals();
    List<HospitalBranch> getBranchesByHospital(Long hospitalId);
    List<Doctor> getDoctorsByBranch(Long branchId);
    List<Doctor> getDoctorsByHospital(Long hospitalId);
    List<Slot> getSlotsByDoctorAndDate(Long doctorId, LocalDate date);
    Appointment bookAppointment(Long userId, Long slotId, Long doctorId, String serviceName);
    void cancelAppointment(Long appointmentId);
}
