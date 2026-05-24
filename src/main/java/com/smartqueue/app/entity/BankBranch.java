package com.smartqueue.app.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "bank_branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankBranch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_id", nullable = false)
    @JsonBackReference
    private Bank bank;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    private Double latitude;
    private Double longitude;
}
