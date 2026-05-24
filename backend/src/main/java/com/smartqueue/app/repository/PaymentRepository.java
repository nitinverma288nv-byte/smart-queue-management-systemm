package com.smartqueue.app.repository;

import com.smartqueue.app.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
