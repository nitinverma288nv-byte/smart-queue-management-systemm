package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {
    List<Queue> findBySectorType(String sectorType);
    List<Queue> findByBranchId(Long branchId);
    Optional<Queue> findByCounterId(Long counterId);
    List<Queue> findByAssignedStaffId(Long assignedStaffId);
    Optional<Queue> findBySectorTypeAndBranchIdAndCounterId(String sectorType, Long branchId, Long counterId);
    Optional<Queue> findBySectorTypeAndCounterId(String sectorType, Long counterId);
}
