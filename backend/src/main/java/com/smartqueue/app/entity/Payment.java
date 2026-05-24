package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sector_type", nullable = false)
    private String sectorType; // HOSPITAL, COLLEGE, BANK

    @Column(nullable = false)
    private Double amount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // UPI, DEBIT_CARD, CREDIT_CARD, NET_BANKING, CASH_COUNTER, QR_PAYMENT

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // PAID, PENDING, FAILED

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
