package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    @Query("SELECT c FROM Counter c WHERE c.sectorType = :type AND c.branchId = :referenceId")
    List<Counter> findByTypeAndReferenceId(@Param("type") String type, @Param("referenceId") Long referenceId);

    List<Counter> findBySectorType(String sectorType);
    List<Counter> findByAssignedStaffId(Long assignedStaffId);
    Optional<Counter> findByCounterNameAndBranchId(String counterName, Long branchId);
}
