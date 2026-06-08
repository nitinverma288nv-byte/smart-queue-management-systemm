package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    List<Slot> findByTypeAndReferenceIdAndDate(String type, Long referenceId, LocalDate date);
    List<Slot> findByTypeAndReferenceId(String type, Long referenceId);
    List<Slot> findByCounterIdAndDateAndAvailabilityTrue(Long counterId, LocalDate date);
    List<Slot> findByCounterIdAndAvailabilityTrue(Long counterId);
    List<Slot> findBySectorTypeAndBranchIdAndDate(String sectorType, Long branchId, LocalDate date);
    List<Slot> findByTypeAndBranchIdAndDate(String type, Long branchId, LocalDate date);
    List<Slot> findByBranchIdAndDate(Long branchId, LocalDate date);
}
