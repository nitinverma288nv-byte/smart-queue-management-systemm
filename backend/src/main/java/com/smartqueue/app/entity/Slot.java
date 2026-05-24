package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // DOCTOR, BANK, COLLEGE

    @Column(nullable = false)
    private Long referenceId; // doctorId or counterId

    @Column(name = "sector_type")
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "counter_id")
    private Long counterId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer maxTokens;

    @Column(nullable = false)
    private Integer bookedTokens;

    @Column(name = "availability")
    @Builder.Default
    private Boolean availability = true;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (availability == null) availability = true;
        if (maxCapacity == null) maxCapacity = maxTokens;
    }
}
