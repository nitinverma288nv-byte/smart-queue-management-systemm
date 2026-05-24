package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "counters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"counter_name", "branch_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Counter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_name", nullable = false)
    private String counterName;

    @Column(name = "counter_type")
    private String counterType; // OPD Counter, Cash Counter, Fees Counter, etc.

    @Column(name = "sector_type", nullable = false)
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(name = "branch_id")
    private Long branchId; // doctorId or bankBranchId or departmentId

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "current_token")
    private String currentToken;

    @Column(name = "queue_size")
    private Integer queueSize;

    @Column(nullable = false)
    private String status; // ACTIVE, INACTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    // Custom Builder class to support backward-compatible builder properties
    public static class CounterBuilder {
        public CounterBuilder name(String name) {
            this.counterName = name;
            return this;
        }

        public CounterBuilder type(String type) {
            this.sectorType = type;
            return this;
        }

        public CounterBuilder referenceId(Long referenceId) {
            this.branchId = referenceId;
            return this;
        }
    }

    // Backward compatibility getters to prevent breaking existing Java business logic
    public String getName() {
        return counterName;
    }

    public String getType() {
        return sectorType;
    }

    public Long getReferenceId() {
        return branchId;
    }
}
