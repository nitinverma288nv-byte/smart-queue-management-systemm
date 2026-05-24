package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queues", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sector_type", "branch_id", "counter_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(nullable = false)
    private Long referenceId; // original referenceId for backward compatibility (branchId or departmentId)

    @Column(name = "branch_id", nullable = false)
    private Long branchId; // hospitalBranchId, bankBranchId, or departmentId

    @Column(name = "counter_id", nullable = false)
    private Long counterId;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "current_serving_token_id")
    private Token currentServingToken;

    @Column(nullable = false)
    private Integer lastTokenNumber; // e.g. 100, which increments to 101, 102

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
