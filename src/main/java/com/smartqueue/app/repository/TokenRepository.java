package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findByUserId(Long userId);
    List<Token> findBySectorTypeAndReferenceIdAndStatus(String sectorType, Long referenceId, String status);
    List<Token> findBySectorTypeAndReferenceId(String sectorType, Long referenceId);
    List<Token> findBySectorTypeAndBranchIdAndCounterIdAndStatus(String sectorType, Long branchId, Long counterId, String status);
    List<Token> findBySectorTypeAndBranchIdAndCounterId(String sectorType, Long branchId, Long counterId);
    List<Token> findBySectorTypeAndBranchIdAndCounterIdAndAssignedStaffIdAndStatus(String sectorType, Long branchId, Long counterId, Long assignedStaffId, String status);
    List<Token> findByCounterIdAndStatus(Long counterId, String status);
    List<Token> findByAssignedStaffIdAndStatus(Long assignedStaffId, String status);
    List<Token> findByBranchIdAndStatus(Long branchId, String status);
    List<Token> findBySectorTypeAndStatus(String sectorType, String status);
    List<Token> findByCreatedAtAfter(LocalDateTime dateTime);
    Optional<Token> findFirstBySectorTypeOrderByIdDesc(String sectorType);
}
