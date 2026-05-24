package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByBranchId(Long branchId);
    List<Doctor> findByHospitalId(Long hospitalId);
}
