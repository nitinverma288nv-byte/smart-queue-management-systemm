package com.smartqueue.app.repository;

import com.smartqueue.app.entity.HospitalBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HospitalBranchRepository extends JpaRepository<HospitalBranch, Long> {
    List<HospitalBranch> findByHospitalId(Long hospitalId);
}
