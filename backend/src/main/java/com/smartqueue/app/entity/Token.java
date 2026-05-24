package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenNumber; // H-101, B-101, C-101

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false)
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(nullable = false)
    private Long referenceId; // original branchId or departmentId for backward compatibility

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "counter_id", nullable = false)
    private Long counterId;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    private String serviceName; // consultation, withdraw, bonafide

    @Column(nullable = false)
    private String status; // WAITING, SERVING, COMPLETED, SKIPPED

    @Column(nullable = false)
    private String priority; // REGULAR, EMERGENCY

    @Transient
    private String doctorName;

    @Transient
    private String hospitalName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
