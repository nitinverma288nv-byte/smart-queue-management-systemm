package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonBackReference
    private HospitalBranch branch;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "specialization")
    private String specialization;

    // Keep for backward compatibility
    @Column(nullable = false)
    private String specialty;

    @Column(name = "consultation_fee")
    private Integer consultationFee;

    @Column(name = "availability_status")
    @Builder.Default
    private Boolean availabilityStatus = true;

    @PrePersist
    @PreUpdate
    protected void onSaveOrUpdate() {
        // Keep specialty and specialization in sync
        if (specialization == null && specialty != null) {
            specialization = specialty;
        }
        if (specialty == null && specialization != null) {
            specialty = specialization;
        }
        // Auto-set hospitalId from branch relationship
        if (hospitalId == null && branch != null && branch.getHospital() != null) {
            hospitalId = branch.getHospital().getId();
        }
    }

    // Getter that prefers specialization for frontend display
    public String getSpecialty() {
        return specialization != null ? specialization : specialty;
    }
}
