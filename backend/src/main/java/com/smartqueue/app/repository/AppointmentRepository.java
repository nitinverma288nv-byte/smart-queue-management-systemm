package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserId(Long userId);
    List<Appointment> findBySectorTypeAndReferenceId(String sectorType, Long referenceId);
    boolean existsByUserIdAndSlotIdAndStatusNot(Long userId, Long slotId, String status);
}
