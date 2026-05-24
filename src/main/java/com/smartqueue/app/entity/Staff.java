package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String sectorType; // HOSPITAL, BANK, COLLEGE

    @Column(nullable = false)
    private Long referenceId; // branchId or departmentId

    @ManyToOne
    @JoinColumn(name = "counter_id")
    private Counter counter;

    @Transient
    private String organizationName;

    @Transient
    private String branchName;
}
