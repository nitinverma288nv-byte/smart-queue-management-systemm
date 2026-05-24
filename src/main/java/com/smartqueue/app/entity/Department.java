package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    @JsonBackReference
    private College college;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String buildingDetails;

    @Column(nullable = false)
    private String officeName;
}
