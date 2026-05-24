package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "hospital_branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalBranch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hospital_id", nullable = false)
    @JsonBackReference
    private Hospital hospital;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    private Double latitude;
    private Double longitude;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Doctor> doctors;
}
