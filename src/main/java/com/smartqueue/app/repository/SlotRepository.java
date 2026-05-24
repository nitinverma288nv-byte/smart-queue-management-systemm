package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByTypeAndReferenceIdAndDate(String type, Long referenceId, LocalDate date);
    List<Slot> findByTypeAndReferenceId(String type, Long referenceId);
    List<Slot> findByCounterIdAndDateAndAvailabilityTrue(Long counterId, LocalDate date);
    List<Slot> findByCounterIdAndAvailabilityTrue(Long counterId);
    List<Slot> findBySectorTypeAndBranchIdAndDate(String sectorType, Long branchId, LocalDate date);
}
