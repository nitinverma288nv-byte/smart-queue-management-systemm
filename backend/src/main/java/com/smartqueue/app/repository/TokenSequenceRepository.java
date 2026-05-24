package com.smartqueue.app.repository;

import com.smartqueue.app.entity.TokenSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TokenSequenceRepository extends JpaRepository<TokenSequence, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TokenSequence s WHERE s.sectorType = :sectorType")
    Optional<TokenSequence> findBySectorTypeForUpdate(String sectorType);
}
